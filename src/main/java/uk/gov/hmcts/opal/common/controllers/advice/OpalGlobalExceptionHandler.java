package uk.gov.hmcts.opal.common.controllers.advice;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.QueryTimeoutException;
import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uk.gov.hmcts.opal.common.contentdigest.InvalidContentDigestException;
import uk.gov.hmcts.opal.common.exception.OpalApiException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Slf4j(topic = "opal.OpalGlobalExceptionHandler")
@ControllerAdvice
@RequiredArgsConstructor
public class OpalGlobalExceptionHandler {

    public static final String DB_UNAVAILABLE_MESSAGE = "Opal database is currently unavailable";
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    private static final Set<Integer> RETRIABLE_HTTP = Set.of(408, 429, 502, 503, 504);
    private static final Set<String> TRANSIENT_SQL_STATES = Set.of("40001", "40P01", "55P03");

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

    @ExceptionHandler(InvalidContentDigestException.class)
    public ResponseEntity<ProblemDetail> handleInvalidContentDigestException(InvalidContentDigestException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ex.getTitle(),
            ex.getDetail(),
            "content-digest",
            false,
            ex
        );
        if (!ex.getSupportedAlgorithms().isEmpty()) {
            problemDetail.setProperty("supported_algorithms", ex.getSupportedAlgorithms());
        }
        return responseWithProblemDetail(HttpStatus.BAD_REQUEST, problemDetail);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Missing Required Header",
            String.format("Required request header \"%s\" is missing", ex.getHeaderName()),
            "missing-header",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.BAD_REQUEST, problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            "You do not have permission to access this resource",
            "forbidden",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.FORBIDDEN, problemDetail);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotAcceptableException(
        HttpMediaTypeNotAcceptableException ex) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.NOT_ACCEPTABLE,
            "Not Acceptable",
            "The requested media type cannot be produced by the server",
            "not-acceptable",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.NOT_ACCEPTABLE, problemDetail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException ex) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.NOT_ACCEPTABLE,
            "Not Acceptable",
            "Invalid parameter value format",
            "type-mismatch",
            false,
            ex
        );
        problemDetail.setProperty("reason", ex.getMessage());
        return responseWithProblemDetail(HttpStatus.NOT_ACCEPTABLE, problemDetail);
    }

    @ExceptionHandler(PropertyValueException.class)
    public ResponseEntity<ProblemDetail> handlePropertyValueException(PropertyValueException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Property Value Error",
            "Invalid or missing value for a required property",
            "property-value-error",
            false,
            ex
        );
        problemDetail.setProperty("entity", ex.getEntityName());
        problemDetail.setProperty("property", ex.getPropertyName());
        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotSupportedException(
        HttpMediaTypeNotSupportedException ex) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Unsupported Media Type",
            "The Content-Type is not supported. Please use application/json",
            "unsupported-media-type",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            "The request body could not be read. It may be missing or invalid JSON.",
            "message-not-readable",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.BAD_REQUEST, problemDetail);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ProblemDetail> handleInvalidDataAccessApiUsageException(
        InvalidDataAccessApiUsageException ex) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "A problem occurred while accessing data",
            "invalid-data-access",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<ProblemDetail> handleInvalidDataAccessResourceUsageException(
        InvalidDataAccessResourceUsageException ex) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "A problem occurred with the requested data resource",
            "invalid-resource-usage",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFoundException(EntityNotFoundException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.NOT_FOUND,
            "Entity Not Found",
            "The requested entity could not be found",
            "entity-not-found",
            false,
            ex
        );
        problemDetail.setProperty("reason", ex.getMessage());
        return responseWithProblemDetail(HttpStatus.NOT_FOUND, problemDetail);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchElementException(NoSuchElementException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.NOT_FOUND,
            "No Value Present",
            "The requested element does not exist",
            "no-such-element",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.NOT_FOUND, problemDetail);
    }

    @ExceptionHandler(OpalApiException.class)
    public ResponseEntity<ProblemDetail> handleOpalApiException(OpalApiException ex) {
        HttpStatus status = ex.getError().getHttpStatus();
        ProblemDetail problemDetail = createProblemDetail(
            status,
            status.getReasonPhrase(),
            "An error occurred while processing your request",
            "opal-api-error",
            false,
            ex
        );
        return responseWithProblemDetail(status, problemDetail);
    }

    @ExceptionHandler({ServletException.class, TransactionSystemException.class, PersistenceException.class})
    public ResponseEntity<ProblemDetail> handleServletExceptions(Exception ex) {
        if (ex instanceof QueryTimeoutException) {
            ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.REQUEST_TIMEOUT,
                "Request Timeout",
                "The request did not receive a response from the database within the timeout period",
                "query-timeout",
                true,
                ex
            );
            return responseWithProblemDetail(HttpStatus.REQUEST_TIMEOUT, problemDetail);
        }

        if (ex instanceof NoResourceFoundException nrfe) {
            ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Not Found",
                "The requested resource could not be found",
                "resource-not-found",
                false,
                nrfe
            );
            return responseWithProblemDetail(HttpStatus.NOT_FOUND, problemDetail);
        }

        if (ex instanceof TransactionSystemException tse) {
            boolean retriable = isTransientSqlState(sqlState(NestedExceptionUtils.getMostSpecificCause(tse)));
            ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Transaction Error",
                "A transaction error occurred while processing your request",
                "transaction-error",
                retriable,
                tse
            );
            return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
        }

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "An unexpected error occurred while processing your request",
            "servlet-error",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ProblemDetail> handleSqlException(SQLException ex) {
        if (isConnectivitySqlException(ex)) {
            ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                DB_UNAVAILABLE_MESSAGE,
                "database-unavailable",
                true,
                ex
            );
            return responseWithProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, problemDetail);
        }

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "A database error occurred while processing your request",
            "database-error",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ProblemDetail> handleDataAccessResourceFailureException(
        DataAccessResourceFailureException ex) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service Unavailable",
            DB_UNAVAILABLE_MESSAGE,
            "database-unavailable",
            true,
            ex
        );
        return responseWithProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, problemDetail);
    }

    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<ProblemDetail> handleLazyInitializationException(LazyInitializationException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "A data access error occurred.",
            "lazy-initialization",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(JpaSystemException.class)
    public ResponseEntity<ProblemDetail> handleJpaSystemException(JpaSystemException ex) {
        boolean retriable = isTransientSqlState(sqlState(NestedExceptionUtils.getMostSpecificCause(ex)));
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "A persistence error occurred while processing your request",
            "jpa-system-error",
            retriable,
            ex
        );
        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ProblemDetail> handleHttpServerErrorException(HttpServerErrorException ex) {
        int upstream = ex.getStatusCode().value();
        boolean retriable = RETRIABLE_HTTP.contains(upstream);
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Downstream Server Error",
            ex.getMessage(),
            "http-server-error",
            retriable,
            ex
        );
        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            "Invalid arguments were provided in the request",
            "illegal-argument",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.BAD_REQUEST, problemDetail);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleObjectOptimisticLockingFailureException(
        ObjectOptimisticLockingFailureException ex) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.CONFLICT,
            "Conflict",
            Optional.ofNullable(ex.getMessage()).orElse("Conflict updating record. Please try again."),
            "optimistic-locking",
            false,
            ex
        );
        problemDetail.setProperty("resourceType", ex.getPersistentClassName());
        problemDetail.setProperty("resourceId", Optional.ofNullable(ex.getIdentifier()).map(Object::toString)
            .orElse(""));
        return responseWithProblemDetail(HttpStatus.CONFLICT, problemDetail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.CONFLICT,
            "Conflict",
            "Data integrity violation with the requested resource",
            "resource-conflict",
            false,
            ex
        );

        if (ex.getCause() instanceof ConstraintViolationException cve) {
            problemDetail.setProperty("constraintViolated", cve.getConstraintName());
        }
        return responseWithProblemDetail(HttpStatus.CONFLICT, problemDetail);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        boolean retriable = RETRIABLE_HTTP.contains(status.value());
        ProblemDetail problemDetail = createProblemDetail(
            status,
            status.getReasonPhrase(),
            Optional.ofNullable(ex.getReason()).orElse(status.getReasonPhrase()),
            "response-status",
            retriable,
            ex
        );
        return responseWithProblemDetail(status, problemDetail);
    }

    @ExceptionHandler(FeignException.Unauthorized.class)
    public ResponseEntity<ProblemDetail> handleFeignExceptionUnauthorized(FeignException.Unauthorized ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.UNAUTHORIZED,
            "Not Authorised for Connection",
            ex.getMessage(),
            "unauthorized",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.UNAUTHORIZED, problemDetail);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ProblemDetail> handleFeignException(FeignException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.status());
        boolean retriable = RETRIABLE_HTTP.contains(status.value());
        ProblemDetail problemDetail = createProblemDetail(
            status,
            "Downstream Service Error",
            "Problem with connecting to a dependant service: " + ex.getMessage(),
            "downstream-service-error",
            retriable,
            ex
        );
        return responseWithProblemDetail(status, problemDetail);
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail,
                                              String typeUri, boolean retry, Throwable exception) {
        return OpalProblemDetailFactory.createProblemDetail(status, title, detail, typeUri, retry, exception, log);
    }

    private ResponseEntity<ProblemDetail> responseWithProblemDetail(HttpStatus status, ProblemDetail problemDetail) {
        return OpalProblemDetailFactory.responseWithProblemDetail(status, problemDetail);
    }

    private static String sqlState(Throwable throwable) {
        if (throwable instanceof SQLException sqlException) {
            return sqlException.getSQLState();
        }

        Throwable cause = throwable == null ? null : throwable.getCause();
        if (cause instanceof SQLException sqlException) {
            return sqlException.getSQLState();
        }

        return null;
    }

    private static boolean isTransientSqlState(String state) {
        return state != null && TRANSIENT_SQL_STATES.contains(state);
    }

    private static boolean isConnectivitySqlException(SQLException ex) {
        if (Optional.ofNullable(ex.getSQLState()).map(state -> state.startsWith("08")).orElse(false)) {
            return true;
        }

        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof ConnectException || cause instanceof UnknownHostException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
