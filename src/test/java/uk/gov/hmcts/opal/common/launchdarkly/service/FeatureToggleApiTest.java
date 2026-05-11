package uk.gov.hmcts.opal.common.launchdarkly.service;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.opal.common.TestConfig;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureToggleAspect;
import uk.gov.hmcts.opal.common.launchdarkly.config.LaunchDarklyProperties;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    FeatureToggleAspect.class,
    FeatureToggleApi.class,
    FeatureToggleApiTest.TestLaunchDarklyProperties.class
})
@Import(TestConfig.class)
@TestPropertySource(properties = {
    "test.feature-default=true",
    "test.placeholder-default=false",
    "test.true-default=true",
    "test.false-default=false"
})
@Isolated
class FeatureToggleApiTest {

    private static final String FAKE_FEATURE = "fake-feature";
    private static final String FAKE_ENVIRONMENT = "fake-env";
    private static final String FAKE_KEY = "fake-key";

    @Autowired
    private FeatureToggleApi featureToggleApi;
    @Autowired
    private LaunchDarklyProperties launchDarklyProperties;

    @MockitoBean
    private LDClient ldClient;

    @BeforeEach
    public void beforeEach() {
        launchDarklyProperties.setEnv(FAKE_ENVIRONMENT);
        launchDarklyProperties.setSdkKey(FAKE_KEY);
        launchDarklyProperties.setEnabled(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnCorrectStateWhenUserIsProvided(Boolean toggleState) {
        LDContext ldContext = LDContext.builder(FAKE_KEY)
            .set("timestamp", String.valueOf(System.currentTimeMillis()))
            .set("environment", FAKE_ENVIRONMENT).build();
        givenToggle(FAKE_FEATURE, toggleState);

        assertThat(featureToggleApi.isFeatureEnabled(FAKE_FEATURE, ldContext)).isEqualTo(toggleState);

        verify(ldClient).boolVariation(FAKE_FEATURE, ldContext, false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnCorrectStateWhenDefaultServiceUser(Boolean toggleState) {
        givenToggle(FAKE_FEATURE, toggleState);

        assertThat(featureToggleApi.isFeatureEnabled(FAKE_FEATURE)).isEqualTo(toggleState);
        verifyBoolVariationCalled(FAKE_FEATURE, List.of("timestamp", "environment"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldUseConfiguredDefaultStateWhenLaunchDarklyIsDisabled(Boolean defaultState) {
        launchDarklyProperties.setEnabled(false);
        launchDarklyProperties.setDefaultFlagValues(Map.of(FAKE_FEATURE, defaultState));

        assertThat(featureToggleApi.isFeatureEnabled(FAKE_FEATURE)).isEqualTo(defaultState);
    }

    @ParameterizedTest
    @ValueSource(strings = {"opal", "legacy"})
    void shouldReturnCorrectStringValueWhenDefaultServiceUser(String toggleState) {
        when(ldClient.stringVariation(eq(FAKE_FEATURE), any(LDContext.class), any()))
            .thenReturn(toggleState);

        assertThat(featureToggleApi.getFeatureValue(FAKE_FEATURE, null)).isEqualTo(toggleState);
    }

    private void givenToggle(String feature, boolean state) {
        when(ldClient.boolVariation(eq(feature), any(LDContext.class), anyBoolean()))
            .thenReturn(state);
    }

    private void verifyBoolVariationCalled(String feature, List<String> customAttributesKeys) {
        ArgumentCaptor<LDContext> ldContextArgumentCaptor = ArgumentCaptor.forClass(LDContext.class);
        verify(ldClient).boolVariation(eq(feature), ldContextArgumentCaptor.capture(), eq(false));

        var capturedLdContext = ldContextArgumentCaptor.getValue();
        assertThat(capturedLdContext.getKey()).isEqualTo(FAKE_KEY);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnCorrectStateWhenUserWithLocationIsProvided(Boolean toggleState) {
        LDContext ldContext = LDContext.builder(FAKE_KEY)
            .set("timestamp", String.valueOf(System.currentTimeMillis()))
            .set("environment", FAKE_ENVIRONMENT)
            .set("location", "000000")
            .build();

        givenToggle(FAKE_FEATURE, toggleState);

        assertThat(featureToggleApi.isFeatureEnabled(FAKE_FEATURE, ldContext)).isEqualTo(toggleState);

        verify(ldClient).boolVariation(FAKE_FEATURE, ldContext, false);
    }

    @Test
    void isFeatureEnabledWithPropertyValueDefault_whenLaunchDarklyIsDisabled_shouldUsePropertyValue() {
        launchDarklyProperties.setEnabled(false);
        assertThat(featureToggleApi.isFeatureEnabledWithPropertyValueDefault(
            FAKE_FEATURE,
            "test.true-default"))
            .isTrue();

        assertThat(featureToggleApi.isFeatureEnabledWithPropertyValueDefault(
            FAKE_FEATURE,
            "test.false-default"))
            .isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isFeatureEnabledWithPropertyValueDefault_whenLaunchDarklyIsDisabledAndPropertyDoesNotExit_shouldUseFallback(
        boolean fallbackState) {
        launchDarklyProperties.setEnabled(false);
        assertThat(featureToggleApi.isFeatureEnabledWithPropertyValueDefault(
            FAKE_FEATURE,
            "test.not-found",
            fallbackState))
            .isEqualTo(fallbackState);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isFeatureEnabledWithPropertyValueDefault_whenLaunchDarklyIsEnabled_shouldUseLaunchDarkley(
        boolean toggleState) {
        launchDarklyProperties.setEnabled(true);

        givenToggle(FAKE_FEATURE, toggleState);

        assertThat(featureToggleApi.isFeatureEnabledWithPropertyValueDefault(
            FAKE_FEATURE, "test." + (!toggleState) + "-default",
            !toggleState
        )).isEqualTo(toggleState);

        verify(ldClient).boolVariation(FAKE_FEATURE, featureToggleApi.createLdContext().build(), !toggleState);
    }


    public static class TestLaunchDarklyProperties extends LaunchDarklyProperties {
        public TestLaunchDarklyProperties() {
            super();
        }
    }


}
