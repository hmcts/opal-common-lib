package uk.gov.hmcts.opal.common.launchdarkly.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Deprecated(forRemoval = true)
/**
 * Use FeatureToggleApi directly instead of this service.
 * This service is a thin wrapper around FeatureToggleApi and does not add any additional functionality.
 * @see FeatureToggleApi
 */
public class FeatureToggleService {

    private final FeatureToggleApi featureToggleApi;

    public boolean isFeatureEnabled(String feature) {
        return this.featureToggleApi.isFeatureEnabled(feature);
    }

    public String getFeatureValue(String feature) {
        return this.featureToggleApi.getFeatureValue(feature, "");
    }

    public String getFeatureValue(String feature, String defaultValue) {
        return this.featureToggleApi.getFeatureValue(feature, defaultValue);
    }

}
