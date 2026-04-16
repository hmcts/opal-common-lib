package uk.gov.hmcts.opal.common.launchdarkly.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeatureToggleServiceTest {

    @Test
    void shouldReturnFlagState() {
        FeatureToggleApi featureToggleApi = mock(FeatureToggleApi.class);
        FeatureToggleService featureToggleService = new FeatureToggleService(featureToggleApi);

        when(featureToggleApi.isFeatureEnabled("test-flag")).thenReturn(true);
        assertTrue(featureToggleService.isFeatureEnabled("test-flag"));

        when(featureToggleApi.isFeatureEnabled("test-flag")).thenReturn(false);
        assertFalse(featureToggleService.isFeatureEnabled("test-flag"));
    }

    @Test
    void shouldReturnFeatureValue() {
        FeatureToggleApi featureToggleApi = mock(FeatureToggleApi.class);
        FeatureToggleService featureToggleService = new FeatureToggleService(featureToggleApi);

        when(featureToggleApi.getFeatureValue("test-flag", "")).thenReturn("opal");

        assertSame("opal", featureToggleService.getFeatureValue("test-flag"));
    }

    @Test
    void shouldReturnFeatureValueWithDefault() {
        FeatureToggleApi featureToggleApi = mock(FeatureToggleApi.class);
        FeatureToggleService featureToggleService = new FeatureToggleService(featureToggleApi);

        when(featureToggleApi.getFeatureValue("test-flag", "default")).thenReturn("opal");

        assertSame("opal", featureToggleService.getFeatureValue("test-flag", "default"));
    }
}
