package uk.gov.hmcts.opal.common.launchdarkly;

public class FeatureDisabledException extends RuntimeException {

    public FeatureDisabledException(String message) {
        super(message);
    }
}
