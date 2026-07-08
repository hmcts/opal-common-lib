package uk.gov.hmcts.opal.common.controllers.advice;

import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import uk.gov.hmcts.opal.common.logging.LogUtil;

import java.net.URI;

public final class OpalProblemDetailFactory {

    private OpalProblemDetailFactory() {
    }

    public static ProblemDetail createProblemDetail(HttpStatus status, String title, String detail,
                                                    String typeUri, boolean retry, Throwable exception,
                                                    Logger log) {
        String opalOperationId = LogUtil.getOrCreateOpalOperationId();
        log.error("Error ID {}:", opalOperationId, exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://hmcts.gov.uk/problems/" + typeUri));
        problemDetail.setInstance(URI.create("https://hmcts.gov.uk/problems/instance/" + opalOperationId));
        problemDetail.setProperty("operation_id", opalOperationId);
        problemDetail.setProperty("retriable", retry);
        return problemDetail;
    }

    public static ResponseEntity<ProblemDetail> responseWithProblemDetail(HttpStatus status,
                                                                          ProblemDetail problemDetail) {
        BodyBuilder builder = ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON);
        return builder.body(problemDetail);
    }
}
