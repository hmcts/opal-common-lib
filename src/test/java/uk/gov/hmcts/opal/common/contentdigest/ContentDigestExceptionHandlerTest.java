package uk.gov.hmcts.opal.common.contentdigest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.opal.common.controllers.advice.OpalGlobalExceptionHandler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ContentDigestExceptionHandlerTest {

    @Test
    void handleContentDigestException_returnsProblemDetail() {
        OpalGlobalExceptionHandler handler = new OpalGlobalExceptionHandler();
        InvalidContentDigestException exception = new InvalidContentDigestException(
            "Digest validation failed",
            "Unsupported digest algorithm: sha-256. Supported algorithms (sha-512).",
            List.of("sha-512"));

        ResponseEntity<ProblemDetail> response = handler.handleInvalidContentDigestException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals("Digest validation failed", response.getBody().getTitle());
        assertEquals(
            "Unsupported digest algorithm: sha-256. Supported algorithms (sha-512).",
            response.getBody().getDetail()
        );
        assertEquals(List.of("sha-512"), response.getBody().getProperties().get("supported_algorithms"));
    }

    @Test
    void handleContentDigestException_withoutSupportedAlgorithms_returnsProblemDetail() {
        OpalGlobalExceptionHandler handler = new OpalGlobalExceptionHandler();
        InvalidContentDigestException exception = new InvalidContentDigestException(
            "Digest validation failed",
            "Body hash did not match for algorithm: sha-512");

        ResponseEntity<ProblemDetail> response = handler.handleInvalidContentDigestException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals("Digest validation failed", response.getBody().getTitle());
        assertEquals("Body hash did not match for algorithm: sha-512", response.getBody().getDetail());
        assertNull(response.getBody().getProperties().get("supported_algorithms"));
    }
}
