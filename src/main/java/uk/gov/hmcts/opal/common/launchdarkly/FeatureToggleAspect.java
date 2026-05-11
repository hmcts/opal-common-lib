package uk.gov.hmcts.opal.common.launchdarkly;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class FeatureToggleAspect {
    private final FeatureToggleApi featureToggleApi;

    @Around("execution(* *(..)) && @annotation(featureToggle)")
    public Object checkFeatureEnabled(ProceedingJoinPoint joinPoint, FeatureToggle featureToggle) throws Throwable {
        boolean featureEnabled = featureToggleApi.isFeatureEnabledWithPropertyValueDefault(
            featureToggle.feature(),
            featureToggle.defaultValueProperty(),
            false
        );
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
}
