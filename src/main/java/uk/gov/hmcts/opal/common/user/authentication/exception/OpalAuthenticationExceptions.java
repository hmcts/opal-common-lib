package uk.gov.hmcts.opal.common.user.authentication.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.common.exceptions.standard.ForbiddenException;
import uk.gov.hmcts.common.exceptions.standard.UnauthorizedException;

import java.io.IOException;
import java.io.PrintWriter;

@Component
@RequiredArgsConstructor
public class OpalAuthenticationExceptions implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        try (PrintWriter writer = response.getWriter()) {
            writer.write(
                objectMapper.writeValueAsString(
                    new UnauthorizedException("Unauthorized", authException.getMessage())
                        .createProblemDetail()
                )
            );
        }
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        try (PrintWriter writer = response.getWriter()) {
            writer.write(
                objectMapper.writeValueAsString(
                    new ForbiddenException("Forbidden", "Forbidden: access is forbidden for this user")
                        .createProblemDetail())
            );
        }
    }
}
