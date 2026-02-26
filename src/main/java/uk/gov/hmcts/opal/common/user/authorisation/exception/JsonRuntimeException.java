package uk.gov.hmcts.opal.common.user.authorisation.exception;

import com.fasterxml.jackson.core.JsonProcessingException;

public class JsonRuntimeException extends RuntimeException {
    public JsonRuntimeException(JsonProcessingException e) {
        super(e);
    }
}
