package uk.gov.hmcts.opal.common.user.authentication.model;

public record JwtValidationResult(boolean valid, String reason) {

}
