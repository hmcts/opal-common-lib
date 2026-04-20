package uk.gov.hmcts.opal.common.controllers.advice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.opal.common.dto.Versioned;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.logging.LogUtil;

import java.net.URI;

@Slf4j(topic = "opal.GlobalExceptionHandler")
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {


    @ExceptionHandler(FeatureDisabledException.class)
    public ResponseEntity<ProblemDetail> handleFeatureDisabledException(FeatureDisabledException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Feature Disabled",
            "The requested feature is not currently available",
            "feature-disabled",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.METHOD_NOT_ALLOWED, problemDetail);
    }



    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail,
                                              String typeUri, boolean retry, Throwable exception) {
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

    private ResponseEntity<ProblemDetail> responseWithProblemDetail(HttpStatus status, ProblemDetail problemDetail) {
        return responseWithProblemDetail(status, problemDetail, null);
    }

    private ResponseEntity<ProblemDetail> responseWithProblemDetail(HttpStatus status, ProblemDetail problemDetail,
        Versioned versioned) {
        BodyBuilder builder = ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON);
        return builder.body(problemDetail);
    }
}
