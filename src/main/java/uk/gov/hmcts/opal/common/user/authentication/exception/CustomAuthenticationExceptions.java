package uk.gov.hmcts.opal.common.user.authentication.exception;

import jakarta.servlet.ServletException;
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
@Component
public class CustomAuthenticationExceptions implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

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
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

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
}
