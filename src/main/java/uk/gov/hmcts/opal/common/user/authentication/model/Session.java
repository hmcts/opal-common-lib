package uk.gov.hmcts.opal.common.user.authentication.model;

public record Session(String sessionId, String accessToken, long accessTokenExpiresIn) {
}
