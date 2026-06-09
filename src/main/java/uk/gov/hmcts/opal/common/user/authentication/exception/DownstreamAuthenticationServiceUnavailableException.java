package uk.gov.hmcts.opal.common.user.authentication.exception;

import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class DownstreamAuthenticationServiceUnavailableException extends InvalidBearerTokenException {
    private final String detailMessage;

    public DownstreamAuthenticationServiceUnavailableException(String message) {
        super(message);
        this.detailMessage = message;
    }

    public DownstreamAuthenticationServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.detailMessage = message;
    }

    @Override
    public String getMessage() {
        return detailMessage;
    }
}
