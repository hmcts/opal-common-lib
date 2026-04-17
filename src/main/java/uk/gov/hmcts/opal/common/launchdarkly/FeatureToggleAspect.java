package uk.gov.hmcts.opal.common.launchdarkly;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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

    @Around("execution(* *(*)) && @annotation(featureToggle)")
    public Object checkFeatureEnabled(ProceedingJoinPoint joinPoint, FeatureToggle featureToggle) throws Throwable {

        if (!properties.getEnabled()) {
            log.debug("Launch darkly is disabled:: so feature toggle is ignoring launch darkly flag "
                         + featureToggle.feature());
            return joinPoint.proceed();
        }

        if (featureToggle.value() && featureToggleApi.isFeatureEnabled(
            featureToggle.feature(),
            featureToggle.defaultValue()
        )) {
            return joinPoint.proceed();
        } else if (!featureToggle.value() && !featureToggleApi.isFeatureEnabled(
            featureToggle.feature(),
            featureToggle.defaultValue()
        )) {
            return joinPoint.proceed();
        } else {
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
        }
        return null;
    }
}
