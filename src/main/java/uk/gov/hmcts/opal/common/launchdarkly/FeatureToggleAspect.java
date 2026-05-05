package uk.gov.hmcts.opal.common.launchdarkly;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.common.launchdarkly.config.LaunchDarklyProperties;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class FeatureToggleAspect {

    private final FeatureToggleApi featureToggleApi;
    private final LaunchDarklyProperties properties;
    private final Environment environment;

    @Around("execution(* *(..)) && @annotation(featureToggle)")
    public Object checkFeatureEnabled(ProceedingJoinPoint joinPoint, FeatureToggle featureToggle) throws Throwable {
        boolean featureEnabled = isFeatureEnabled(featureToggle);
        if (featureToggle.value() == featureEnabled) {
            return joinPoint.proceed();
        }

        String message = String.format(
            "Feature %s is not enabled for method %s",
            featureToggle.feature(),
            joinPoint.getSignature().getName()
        );
        log.warn(message);
        if (featureToggle.throwException() != null) {
            throw featureToggle.throwException()
                .getConstructor(String.class)
                .newInstance(message);
        }

        return null;
    }

    private boolean isFeatureEnabled(FeatureToggle featureToggle) {
        boolean defaultValue = resolveDefaultValue(featureToggle);
        if (!properties.isEnabled()) {
            log.debug("Launch darkly is disabled: using default value fallback {} for feature {}",
                defaultValue, featureToggle.feature());
            return defaultValue;
        }

        return featureToggleApi.isFeatureEnabled(featureToggle.feature(), defaultValue);
    }

    private boolean resolveDefaultValue(FeatureToggle featureToggle) {
        String defaultValueProperty = featureToggle.defaultValueProperty();
        if (defaultValueProperty == null || defaultValueProperty.isBlank()) {
            return false;
        }

        String resolvedValue = environment.resolvePlaceholders(defaultValueProperty);
        if (!resolvedValue.equals(defaultValueProperty)) {
            return Boolean.parseBoolean(resolvedValue);
        }

        return Boolean.parseBoolean(environment.getProperty(defaultValueProperty, "false"));
    }
}
