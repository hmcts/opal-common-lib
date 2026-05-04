package uk.gov.hmcts.opal.common.launchdarkly;

import com.launchdarkly.sdk.server.LDClient;
import org.aspectj.lang.annotation.Around;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.opal.common.launchdarkly.config.LaunchDarklyProperties;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    FeatureToggleAspect.class,
    FeatureToggleApi.class
})
@Isolated
class FeatureToggleAspectTest {

    private static final String NEW_FEATURE = "NEW_FEATURE";
    private static final String NEW_FEATURE_PROPERTY = "feature.flags.new-feature.enabled";
    private static final String EXCEPTION = "Feature NEW_FEATURE is not enabled for method myFeatureToggledMethod";

    @Autowired
    FeatureToggleAspect featureToggleAspect;

    @Autowired
    FeatureToggleApi featureToggleApi;

    @MockitoBean
    LaunchDarklyProperties properties;

    @MockitoBean
    Environment environment;

    @MockitoBean
    LDClient ldClient;

    @MockitoBean
    ProceedingJoinPoint proceedingJoinPoint;

    @MockitoBean
    FeatureToggle featureToggle;

    @MockitoBean
    MethodSignature methodSignature;

    @BeforeEach
    void setUp() {
        when(featureToggle.feature()).thenReturn(NEW_FEATURE);
        when(featureToggle.defaultProperty()).thenReturn("");
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("myFeatureToggledMethod");
        when(properties.isEnabled()).thenReturn(true);
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldProceedToMethodInvocationWhenFeatureToggleIsEnabled(Boolean state) {
        when(featureToggle.value()).thenReturn(state);
        givenToggle(state);

        featureToggleAspect.checkFeatureEnabled(proceedingJoinPoint, featureToggle);

        verify(proceedingJoinPoint).proceed();
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldProceedToMethodInvocationWhenLaunchDarklyIsDisabledAndDefaultValueMatchesExpectedState(Boolean state) {
        when(featureToggle.value()).thenReturn(state);
        when(featureToggle.defaultValue()).thenReturn(state);
        when(properties.isEnabled()).thenReturn(false);

        featureToggleAspect.checkFeatureEnabled(proceedingJoinPoint, featureToggle);

        verify(proceedingJoinPoint).proceed();
        verify(ldClient, never()).boolVariation(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldNotProceedToMethodInvocationWhenLaunchDarklyIsDisabledAndDefaultValueDoesNotMatchExpectedState(
        Boolean state
    ) {
        when(featureToggle.value()).thenReturn(state);
        when(featureToggle.defaultValue()).thenReturn(!state);
        when(properties.isEnabled()).thenReturn(false);

        featureToggleAspect.checkFeatureEnabled(proceedingJoinPoint, featureToggle);

        verify(proceedingJoinPoint, never()).proceed();
        verify(ldClient, never()).boolVariation(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldProceedToMethodInvocationWhenLaunchDarklyIsDisabledAndDefaultPropertyMatchesExpectedState(Boolean state) {
        when(featureToggle.value()).thenReturn(state);
        when(featureToggle.defaultValue()).thenReturn(!state);
        when(featureToggle.defaultProperty()).thenReturn(NEW_FEATURE_PROPERTY);
        when(environment.getProperty(NEW_FEATURE_PROPERTY, Boolean.class, !state)).thenReturn(state);
        when(properties.isEnabled()).thenReturn(false);

        featureToggleAspect.checkFeatureEnabled(proceedingJoinPoint, featureToggle);

        verify(proceedingJoinPoint).proceed();
        verify(ldClient, never()).boolVariation(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldNotProceedToMethodInvocationWhenLaunchDarklyIsDisabledAndDefaultPropertyDoesNotMatchExpectedState(
        Boolean state
    ) {
        when(featureToggle.value()).thenReturn(state);
        when(featureToggle.defaultValue()).thenReturn(state);
        when(featureToggle.defaultProperty()).thenReturn(NEW_FEATURE_PROPERTY);
        when(environment.getProperty(NEW_FEATURE_PROPERTY, Boolean.class, state)).thenReturn(!state);
        when(properties.isEnabled()).thenReturn(false);

        featureToggleAspect.checkFeatureEnabled(proceedingJoinPoint, featureToggle);

        verify(proceedingJoinPoint, never()).proceed();
        verify(ldClient, never()).boolVariation(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldFallBackToDefaultValueWhenDefaultPropertyIsMissing(Boolean state) {
        FeatureToggleAspect aspect = new FeatureToggleAspect(featureToggleApi, properties, new MockEnvironment());

        when(featureToggle.value()).thenReturn(state);
        when(featureToggle.defaultValue()).thenReturn(state);
        when(featureToggle.defaultProperty()).thenReturn(NEW_FEATURE_PROPERTY);
        when(properties.isEnabled()).thenReturn(false);

        aspect.checkFeatureEnabled(proceedingJoinPoint, featureToggle);

        verify(proceedingJoinPoint).proceed();
        verify(ldClient, never()).boolVariation(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldThrowExceptionWhenFeatureToggleIsDisabled(Boolean state) {
        when(featureToggle.value()).thenReturn(state);
        when(featureToggle.throwException()).thenAnswer(invocation -> FeatureDisabledException.class);
        givenToggle(!state);

        FeatureDisabledException exception = assertThrows(
            FeatureDisabledException.class,
            () -> featureToggleAspect.checkFeatureEnabled(proceedingJoinPoint, featureToggle)
        );

        assertNotNull(exception);
        assertEquals(EXCEPTION, exception.getMessage());
    }

    @Test
    void shouldThrowFeatureDisabledExceptionByDefault() throws NoSuchMethodException {
        when(properties.isEnabled()).thenReturn(false);

        FeatureToggle featureToggle = defaultExceptionAnnotation();

        FeatureDisabledException exception = assertThrows(
            FeatureDisabledException.class,
            () -> featureToggleAspect.checkFeatureEnabled(proceedingJoinPoint, featureToggle)
        );

        assertEquals(EXCEPTION, exception.getMessage());
    }

    @Test
    void shouldMatchFeatureToggleMethodsWithAnyNumberOfArguments() throws NoSuchMethodException {
        Method checkFeatureEnabledMethod = FeatureToggleAspect.class.getMethod(
            "checkFeatureEnabled",
            ProceedingJoinPoint.class,
            FeatureToggle.class
        );
        Around around = checkFeatureEnabledMethod.getAnnotation(Around.class);

        assertEquals("execution(* *(..)) && @annotation(featureToggle)", around.value());
    }

    @Test
    void shouldInterceptMultiArgumentMethod() {
        when(properties.isEnabled()).thenReturn(false);

        MultiArgumentFeatureService target = new MultiArgumentFeatureService();
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(new FeatureToggleAspect(featureToggleApi, properties, new MockEnvironment()));
        MultiArgumentFeatureService proxy = proxyFactory.getProxy();

        assertThrows(FeatureDisabledException.class, () -> proxy.multiArgumentMethod("alpha", 2));
    }

    private void givenToggle(boolean state) {
        when(ldClient.boolVariation(NEW_FEATURE, null, false)).thenReturn(state);
        when(ldClient.boolVariation(org.mockito.ArgumentMatchers.eq(NEW_FEATURE),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(state);
    }

    private FeatureToggle defaultExceptionAnnotation() throws NoSuchMethodException {
        return AnnotatedFeatureService.class.getMethod("defaultExceptionMethod").getAnnotation(FeatureToggle.class);
    }

    static class AnnotatedFeatureService {

        @SuppressWarnings("unused")
        @FeatureToggle(feature = NEW_FEATURE, defaultValue = false)
        public void defaultExceptionMethod() {
        }
    }

    static class MultiArgumentFeatureService {

        @FeatureToggle(feature = NEW_FEATURE, defaultValue = false)
        public void multiArgumentMethod(String value, Integer version) {
            if (value == null || version == null) {
                throw new IllegalArgumentException("Test arguments must be non-null");
            }
        }
    }
}
