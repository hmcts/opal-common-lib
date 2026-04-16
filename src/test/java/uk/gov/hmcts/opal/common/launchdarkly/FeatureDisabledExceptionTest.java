package uk.gov.hmcts.opal.common.launchdarkly;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeatureDisabledExceptionTest {

    @Test
    void shouldSetMessage() {
        String errorMessage = "Feature disabled";
        FeatureDisabledException exception = new FeatureDisabledException(errorMessage);

        assertEquals(errorMessage, exception.getMessage());
    }
}
