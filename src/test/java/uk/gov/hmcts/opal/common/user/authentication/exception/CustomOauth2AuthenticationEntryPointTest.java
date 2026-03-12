package uk.gov.hmcts.opal.common.user.authentication.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import uk.gov.hmcts.opal.common.logging.LogUtil;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.opal.common.dto.ToJsonString;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOauth2AuthenticationEntryPointTest {

    @Mock
    SecurityEventLoggingService securityEventLoggingService;

    @Mock
    AccessTokenService tokenService;

    @InjectMocks
    CustomOauth2AuthenticationEntryPoint entryPoint;

    @Test
    void commenceShouldUseOidAsUserIdentifierWhenPresent() throws Exception {

        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authenticationException = mock(AuthenticationException.class);
        when(authenticationException.getMessage()).thenReturn("err_message");

        StringWriter output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
        when(request.getHeader(AccessTokenService.AUTH_HEADER)).thenReturn("Bearer token-value");
        when(tokenService.extractOid("Bearer token-value")).thenReturn("oid-123");
        when(request.getRequestURI()).thenReturn("/test/resource");

        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 10, 0);
        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getRequestTimestamp).thenReturn(timestamp);
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn("op-123");

            // Act
            entryPoint.commence(request, response, authenticationException);
        }

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");

        ProblemDetail body = ToJsonString.getObjectMapper().readValue(output.toString(), ProblemDetail.class);
        assertNotNull(body);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.getStatus());
        assertEquals("Unauthorized", body.getTitle());
        assertEquals("You are not authorized to access this resource", body.getDetail());
        assertEquals("https://hmcts.gov.uk/problems/unauthorized", body.getType().toString());
        assertEquals("op-123", body.getProperties().get("operation_id"));
        assertEquals(false, body.getProperties().get("retriable"));
        assertEquals(401, body.getStatus());
        assertEquals("Unauthorized", body.getTitle());

        verify(securityEventLoggingService).logEvent(
            eq("Authorisation Access Control"),
            eq("Failure"),
            eq(null),
            eq("Authentication"),
            eq(timestamp),
            argThat(eventData -> "oid-123".equals(eventData.get("UserIdentifier"))
                && "err_message".equals(eventData.get("Details"))
                && "/test/resource".equals(eventData.get("Resource"))));
    }

    @Test
    void commenceShouldFallbackToRemoteAddressWhenOidExtractionFails() throws Exception {

        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authenticationException = mock(AuthenticationException.class);
        when(authenticationException.getMessage()).thenReturn("err_message");

        StringWriter output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
        when(request.getHeader(AccessTokenService.AUTH_HEADER)).thenReturn("Bearer bad-token");
        when(tokenService.extractOid("Bearer bad-token")).thenThrow(new RuntimeException("bad token"));
        when(request.getRequestURI()).thenReturn("/test/fallback");
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");

        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 10, 0);
        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getRequestTimestamp).thenReturn(timestamp);
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn("op-456");

            // Act
            entryPoint.commence(request, response, authenticationException);
        }

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");

        ProblemDetail body = ToJsonString.getObjectMapper().readValue(output.toString(), ProblemDetail.class);
        assertNotNull(body);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.getStatus());
        assertEquals("Unauthorized", body.getTitle());
        assertEquals("You are not authorized to access this resource", body.getDetail());
        assertEquals("https://hmcts.gov.uk/problems/unauthorized", body.getType().toString());
        assertEquals("op-456", body.getProperties().get("operation_id"));
        assertEquals(false, body.getProperties().get("retriable"));
        assertEquals(401, body.getStatus());
        assertEquals("Unauthorized", body.getTitle());

        verify(securityEventLoggingService).logEvent(
            eq("Authorisation Access Control"),
            eq("Failure"),
            eq(null),
            eq("Authentication"),
            eq(timestamp),
            argThat(eventData -> "192.168.1.10".equals(eventData.get("UserIdentifier"))
                && "err_message".equals(eventData.get("Details"))
                && "/test/fallback".equals(eventData.get("Resource"))));
    }
}
