package uk.gov.hmcts.opal.common.user.authentication.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import uk.gov.hmcts.opal.common.dto.ToJsonString;
import uk.gov.hmcts.opal.common.logging.LogUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomAuthenticationExceptionsTest {

    private CustomAuthenticationExceptions customAuthenticationExceptions;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter output;

    @BeforeEach
    void setUp() throws IOException {
        customAuthenticationExceptions = new CustomAuthenticationExceptions();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
    }

    @Test
    void commenceShouldReturnUnauthorizedProblemDetail() throws IOException {
        AuthenticationException authException = mock(AuthenticationException.class);

        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn("op-auth");

            customAuthenticationExceptions.commence(request, response, authException);
        }

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail body = ToJsonString.getObjectMapper().readValue(output.toString(), ProblemDetail.class);
        assertNotNull(body);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.getStatus());
        assertEquals("Unauthorized", body.getTitle());
        assertEquals("https://hmcts.gov.uk/problems/unauthorized", body.getType().toString());
        assertEquals("op-auth", body.getProperties().get("operation_id"));
        assertEquals(false, body.getProperties().get("retriable"));
    }

    @Test
    void handleShouldReturnForbiddenProblemDetail() throws IOException {
        AccessDeniedException accessDeniedException = mock(AccessDeniedException.class);

        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn("op-forbidden");

            customAuthenticationExceptions.handle(request, response, accessDeniedException);
        }

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail body = ToJsonString.getObjectMapper().readValue(output.toString(), ProblemDetail.class);
        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
        assertEquals("Forbidden", body.getTitle());
        assertEquals("https://hmcts.gov.uk/problems/forbidden", body.getType().toString());
        assertEquals("op-forbidden", body.getProperties().get("operation_id"));
        assertEquals(false, body.getProperties().get("retriable"));
    }

    @Test
    void commenceShouldReturnNotFoundWhenEntityNotFoundIsInCauseChain() throws IOException {
        EntityNotFoundException entityNotFoundException = new EntityNotFoundException("User record not found");
        AuthenticationException authException = new AuthenticationException(
            "Authentication failed",
            new RuntimeException(entityNotFoundException)
        ) {
        };

        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn("op-auth-404");

            customAuthenticationExceptions.commence(request, response, authException);
        }

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail body = ToJsonString.getObjectMapper().readValue(output.toString(), ProblemDetail.class);
        assertNotNull(body);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
        assertEquals("Entity Not Found", body.getTitle());
        assertEquals("https://hmcts.gov.uk/problems/entity-not-found", body.getType().toString());
        assertEquals("User record not found", body.getProperties().get("reason"));
        assertEquals("op-auth-404", body.getProperties().get("operation_id"));
        assertEquals(false, body.getProperties().get("retriable"));
    }

    @Test
    void handleShouldReturnNotFoundWhenEntityNotFoundIsInCauseChain() throws IOException {
        EntityNotFoundException entityNotFoundException = new EntityNotFoundException("Permission target not found");
        AccessDeniedException accessDeniedException = new AccessDeniedException(
            "Access denied",
            new RuntimeException(entityNotFoundException)
        );

        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn("op-forbidden-404");

            customAuthenticationExceptions.handle(request, response, accessDeniedException);
        }

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail body = ToJsonString.getObjectMapper().readValue(output.toString(), ProblemDetail.class);
        assertNotNull(body);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
        assertEquals("Entity Not Found", body.getTitle());
        assertEquals("https://hmcts.gov.uk/problems/entity-not-found", body.getType().toString());
        assertEquals("Permission target not found", body.getProperties().get("reason"));
        assertEquals("op-forbidden-404", body.getProperties().get("operation_id"));
        assertEquals(false, body.getProperties().get("retriable"));
    }
}
