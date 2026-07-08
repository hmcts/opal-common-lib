package uk.gov.hmcts.opal.common.user.authentication.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.common.controllers.advice.OpalProblemDetailFactory;
import uk.gov.hmcts.opal.common.dto.ToJsonString;

import java.io.IOException;
import java.io.PrintWriter;

@Slf4j(topic = "opal.CustomAuthenticationExceptions")
@Component("customAuthenticationExceptions2")
public class CustomAuthenticationExceptions implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException) throws IOException {
        if (checkForNotFound(response, authException)) {
            return;
        }
        ProblemDetail problemDetail = OpalProblemDetailFactory.createProblemDetail(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            "You are not authorized to access this resource",
            "unauthorized",
            false,
            authException,
            log
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(ToJsonString.toJsonString(problemDetail));
        }
    }


    @Override
    public void handle(HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException) throws IOException {
        if (checkForNotFound(response, accessDeniedException)) {
            return;
        }
        ProblemDetail problemDetail = OpalProblemDetailFactory.createProblemDetail(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            "You do not have permission to access this resource",
            "forbidden",
            false,
            accessDeniedException,
            log
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(ToJsonString.toJsonString(problemDetail));
        }
    }


    private boolean checkForNotFound(HttpServletResponse response, Exception exception)
        throws IOException {
        log.debug("Checking if the exception is or wraps an EntityNotFoundException");
        EntityNotFoundException entityNotFoundException = findEntityNotFoundCause(exception);
        if (entityNotFoundException == null) {
            return false;
        }

        log.debug("Exception chain contains EntityNotFoundException, returning 404 Not Found");
        ProblemDetail problemDetail = OpalProblemDetailFactory.createProblemDetail(
            HttpStatus.NOT_FOUND,
            "Entity Not Found",
            "The requested entity could not be found",
            "entity-not-found",
            false,
            entityNotFoundException,
            log
        );
        problemDetail.setProperty("reason", entityNotFoundException.getMessage());

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(ToJsonString.toJsonString(problemDetail));
        }
        return true;
    }

    private EntityNotFoundException findEntityNotFoundCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof EntityNotFoundException entityNotFoundException) {
                return entityNotFoundException;
            }
            current = current.getCause();
        }
        return null;
    }
}
