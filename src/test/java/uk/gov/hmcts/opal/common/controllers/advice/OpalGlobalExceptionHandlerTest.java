package uk.gov.hmcts.opal.common.controllers.advice;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.QueryTimeoutException;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uk.gov.hmcts.opal.common.exception.DownstreamServiceUnavailableException;
import uk.gov.hmcts.opal.common.exception.OpalApiException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.user.authentication.exception.AuthenticationError;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class OpalGlobalExceptionHandlerTest {

    private final OpalGlobalExceptionHandler handler = new OpalGlobalExceptionHandler();

    @Test
    void handleMissingRequestHeader_returnsBadRequestProblemDetail() throws Exception {
        Method m = TestMissingHeaderClass.class.getMethod("testMethod", String.class);

        MissingRequestHeaderException ex = new MissingRequestHeaderException(
            "Authorization",
            new org.springframework.core.MethodParameter(m, 0));

        ResponseEntity<ProblemDetail> response = handler.handleMissingRequestHeaderException(ex);

        assertProblem(response, HttpStatus.BAD_REQUEST, "Missing Required Header", "missing-header", false);
    }

    @Test
    void handleAccessDenied_returnsForbiddenProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleAccessDeniedException(
            new AccessDeniedException("nope"));

        assertProblem(response, HttpStatus.FORBIDDEN, "Forbidden", "forbidden", false);
    }

    @Test
    void handleFeatureDisabled_returnsNotFoundProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleFeatureDisabledException(
            new FeatureDisabledException("disabled"));

        assertProblem(response, HttpStatus.NOT_FOUND, "Feature Disabled", "feature-disabled", false);
    }

    @Test
    void handleCommonRequestErrors_areNonRetriable() throws Exception {
        ResponseEntity<ProblemDetail> notAcceptable = handler.handleHttpMediaTypeNotAcceptableException(
            new HttpMediaTypeNotAcceptableException("nope"));
        assertProblem(notAcceptable, HttpStatus.NOT_ACCEPTABLE, "Not Acceptable", "not-acceptable", false);

        Method method = OpalGlobalExceptionHandlerTest.class.getMethod("sampleMethod", Integer.class);
        MethodArgumentTypeMismatchException typeMismatch = new MethodArgumentTypeMismatchException(
            "x",
            Integer.class,
            "sample",
            new org.springframework.core.MethodParameter(method, 0),
            new NumberFormatException("bad"));
        assertProblem(handler.handleMethodArgumentTypeMismatchException(typeMismatch), HttpStatus.NOT_ACCEPTABLE,
                      "Not Acceptable", "type-mismatch", false);

        HttpInputMessage inputMessage = mock(HttpInputMessage.class);
        assertProblem(handler.handleHttpMessageNotReadableException(new HttpMessageNotReadableException("bad",
                                                                                                        inputMessage)),
                      HttpStatus.BAD_REQUEST, "Bad Request", "message-not-readable", false);
        assertProblem(handler.handleHttpMediaTypeNotSupportedException(new HttpMediaTypeNotSupportedException("bad")),
                      HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", "unsupported-media-type", false);
    }

    @Test
    void handlePersistenceLookupErrors_returnExpectedProblemDetails() throws Exception {
        assertProblem(handler.handleEntityNotFoundException(new EntityNotFoundException("missing")),
                      HttpStatus.NOT_FOUND, "Entity Not Found", "entity-not-found", false);
        assertProblem(handler.handleNoSuchElementException(new NoSuchElementException("missing")),
                      HttpStatus.NOT_FOUND, "No Value Present", "no-such-element", false);
        assertProblem(handler.handleServletExceptions(new NoResourceFoundException(HttpMethod.GET, "/x", "missing")),
                      HttpStatus.NOT_FOUND, "Not Found", "resource-not-found", false);
    }

    @Test
    void handleTimeoutAndTransientDatabaseErrors_areRetriable() {
        assertProblem(handler.handleServletExceptions(new QueryTimeoutException("timeout", null, null)),
                      HttpStatus.REQUEST_TIMEOUT, "Request Timeout", "query-timeout", true);
        assertProblem(handler.handleSqlException(new SQLException("connect", "08001")),
                      HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", "database-unavailable", true);
        assertProblem(handler.handleDataAccessResourceFailureException(new DataAccessResourceFailureException("down")),
                      HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", "database-unavailable", true);

        TransactionSystemException transaction = new TransactionSystemException(
            "tx",
            new SQLException("deadlock", "40P01"));
        assertProblem(handler.handleServletExceptions(transaction), HttpStatus.INTERNAL_SERVER_ERROR,
                      "Transaction Error", "transaction-error", true);

        JpaSystemException jpa = new JpaSystemException(new RuntimeException(new SQLException("serial", "40001")));
        assertProblem(handler.handleJpaSystemException(jpa), HttpStatus.INTERNAL_SERVER_ERROR,
                      OpalGlobalExceptionHandler.INTERNAL_SERVER_ERROR, "jpa-system-error", true);
    }

    @Test
    void handleNonTransientDatabaseErrors_areNotRetriable() {
        assertProblem(handler.handleSqlException(new SQLException("syntax", "42601")),
                      HttpStatus.INTERNAL_SERVER_ERROR, OpalGlobalExceptionHandler.INTERNAL_SERVER_ERROR,
                      "database-error", false);
        assertProblem(handler.handleServletExceptions(new PersistenceException("oops")),
                      HttpStatus.INTERNAL_SERVER_ERROR, OpalGlobalExceptionHandler.INTERNAL_SERVER_ERROR,
                      "servlet-error", false);
    }

    @Test
    void handleOrmAndConflictErrors_includeContextAndNoRetry() {
        PropertyValueException property = new PropertyValueException("missing", "Entity", "name");
        ResponseEntity<ProblemDetail> propertyResponse = handler.handlePropertyValueException(property);
        assertProblem(propertyResponse, HttpStatus.INTERNAL_SERVER_ERROR, "Property Value Error",
                      "property-value-error", false);
        assertEquals("Entity", propertyResponse.getBody().getProperties().get("entity"));
        assertEquals("name", propertyResponse.getBody().getProperties().get("property"));

        ObjectOptimisticLockingFailureException optimisticLock = new ObjectOptimisticLockingFailureException(
            String.class,
            "123");
        ResponseEntity<ProblemDetail> conflict = handler.handleObjectOptimisticLockingFailureException(optimisticLock);
        assertProblem(conflict, HttpStatus.CONFLICT, "Conflict", "optimistic-locking", false);
        assertEquals(String.class.getName(), conflict.getBody().getProperties().get("resourceType"));
        assertEquals("123", conflict.getBody().getProperties().get("resourceId"));
    }

    @Test
    void handleDataIntegrityViolation_includesConstraintNameWhenAvailable() {
        ConstraintViolationException constraint = new ConstraintViolationException(
            "message",
            null,
            ConstraintViolationException.ConstraintKind.OTHER,
            "constraint-name");

        ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolationException(
            new DataIntegrityViolationException("bad", constraint));

        assertProblem(response, HttpStatus.CONFLICT, "Conflict", "resource-conflict", false);
        assertEquals("constraint-name", response.getBody().getProperties().get("constraintViolated"));
    }

    @Test
    void handleDownstreamErrors_setsRetryFromStatus() {
        HttpServerErrorException http503 = HttpServerErrorException.create(
            HttpStatusCode.valueOf(503),
            "Service Unavailable",
            HttpHeaders.EMPTY,
            null,
            null);
        assertProblem(handler.handleHttpServerErrorException(http503), HttpStatus.INTERNAL_SERVER_ERROR,
                      "Downstream Server Error", "http-server-error", true);

        assertProblem(handler.handleFeignException(buildFeignException(503, "Service Unavailable")),
                      HttpStatus.SERVICE_UNAVAILABLE, "Downstream Service Error", "downstream-service-error", true);
        assertProblem(handler.handleFeignException(buildFeignException(404, "Not Found")),
                      HttpStatus.NOT_FOUND, "Downstream Service Error", "downstream-service-error", false);
    }

    @Test
    void handleDownstreamServiceUnavailable_returnsServiceUnavailableProblemDetail() {
        assertProblem(
            handler.handleDownstreamServiceUnavailableException(
                new DownstreamServiceUnavailableException("The required user-service endpoint is disabled.")
            ),
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service Unavailable",
            "downstream-service-unavailable",
            false
        );
    }

    @Test
    void handleResponseStatusException_returnsProblemJsonWithRetryFlag() {
        assertProblem(handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "down")),
                      HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", "response-status", true);
        assertProblem(handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad")),
                      HttpStatus.BAD_REQUEST, "Bad Request", "response-status", false);
    }

    @Test
    void handleOpalApiException_usesErrorStatus() {
        OpalApiException exception = new OpalApiException(
            AuthenticationError.FAILED_TO_OBTAIN_AUTHENTICATION_CONFIG);

        assertProblem(handler.handleOpalApiException(exception), HttpStatus.INTERNAL_SERVER_ERROR,
                      OpalGlobalExceptionHandler.INTERNAL_SERVER_ERROR, "opal-api-error", false);
    }

    public static void sampleMethod(Integer testParam) {
        // Used to create a MethodParameter in tests.
    }

    private static void assertProblem(ResponseEntity<ProblemDetail> response, HttpStatus status, String title,
                                      String type, boolean retriable) {
        assertEquals(status, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        ProblemDetail problemDetail = response.getBody();
        assertNotNull(problemDetail);
        assertEquals(status.value(), problemDetail.getStatus());
        assertEquals(title, problemDetail.getTitle());
        assertEquals("https://hmcts.gov.uk/problems/" + type, problemDetail.getType().toString());
        assertNotNull(problemDetail.getProperties().get("operation_id"));
        assertEquals(retriable, problemDetail.getProperties().get("retriable"));
    }

    private static FeignException buildFeignException(int status, String reason) {
        Map<String, Collection<String>> headers = Collections.emptyMap();
        Request request = Request.create(
            Request.HttpMethod.GET,
            "/test",
            headers,
            Request.Body.empty(),
            new RequestTemplate()
        );

        Response response = Response.builder()
            .request(request)
            .status(status)
            .reason(reason)
            .headers(headers)
            .build();

        return FeignException.errorStatus("GET /test", response);
    }

    static class TestMissingHeaderClass {
        public void testMethod(String type) {
            // Used to create a MethodParameter in tests.
        }
    }
}
