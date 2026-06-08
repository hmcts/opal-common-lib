package uk.gov.hmcts.opal.common.user.authorisation.exception;

import tools.jackson.core.JacksonException;

public class JsonRuntimeException extends RuntimeException {
    public JsonRuntimeException(JacksonException e) {
        super(e);
    }
}
