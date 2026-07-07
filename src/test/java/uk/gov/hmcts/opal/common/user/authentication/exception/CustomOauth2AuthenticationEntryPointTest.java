package uk.gov.hmcts.opal.common.user.authentication.exception;

import tools.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import uk.gov.hmcts.opal.common.logging.LogUtil;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.opal.common.dto.ToJsonString;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

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
        when(response.getHeader("operation_id")).thenReturn("op-123");
        when(request.getHeader(AccessTokenService.AUTH_HEADER)).thenReturn("Bearer token-value");
        when(tokenService.extractOid("Bearer token-value")).thenReturn("oid-123");
        when(request.getRequestURI()).thenReturn("/test/resource");

        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 10, 0);
        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getRequestTimestamp).thenReturn(timestamp);

            // Act
            entryPoint.commence(request, response, authenticationException);
        }

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        JsonNode body = ToJsonString.getObjectMapper().readTree(output.toString());
        Assertions.assertNotNull(body);
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED.value(), body.get("status").asInt());
        Assertions.assertEquals("Unauthorized", body.get("title").asText());
        Assertions.assertEquals("You are not authorized to access this resource", body.get("detail").asText());
        Assertions.assertEquals("https://hmcts.gov.uk/problems/unauthorized", body.get("type").asText());
        Assertions.assertEquals("op-123", body.get("operation_id").asText());
        Assertions.assertFalse(body.get("retriable").asBoolean());
        Assertions.assertEquals("Unauthorized", body.get("title").asText());

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
        when(response.getHeader("operation_id")).thenReturn("op-456");
        when(request.getHeader(AccessTokenService.AUTH_HEADER)).thenReturn("Bearer bad-token");
        when(tokenService.extractOid("Bearer bad-token")).thenThrow(new RuntimeException("bad token"));
        when(request.getRequestURI()).thenReturn("/test/fallback");
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");

        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 10, 0);
        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getRequestTimestamp).thenReturn(timestamp);

            // Act
            entryPoint.commence(request, response, authenticationException);
        }

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        JsonNode body = ToJsonString.getObjectMapper().readTree(output.toString());
        Assertions.assertNotNull(body);
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED.value(), body.get("status").asInt());
        Assertions.assertEquals("Unauthorized", body.get("title").asText());
        Assertions.assertEquals("You are not authorized to access this resource", body.get("detail").asText());
        Assertions.assertEquals("https://hmcts.gov.uk/problems/unauthorized", body.get("type").asText());
        Assertions.assertEquals("op-456", body.get("operation_id").asText());
        Assertions.assertFalse(body.get("retriable").asBoolean());
        Assertions.assertEquals("Unauthorized", body.get("title").asText());

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

    @Test
    void commenceShouldReturnServiceUnavailableProblemDetailForDisabledUserServiceEndpoint() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        DownstreamAuthenticationServiceUnavailableException authenticationException =
            new DownstreamAuthenticationServiceUnavailableException(
                "Authentication was not possible because the required user-service endpoint is disabled."
            );

        StringWriter output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
        when(response.getHeader("operation_id")).thenReturn("op-789");
        when(request.getHeader(AccessTokenService.AUTH_HEADER)).thenReturn("Bearer token-value");
        when(tokenService.extractOid("Bearer token-value")).thenReturn("oid-123");
        when(request.getRequestURI()).thenReturn("/test/disabled-user-service");

        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 3, 10, 0);
        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getRequestTimestamp).thenReturn(timestamp);

            entryPoint.commence(request, response, authenticationException);
        }

        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        JsonNode body = ToJsonString.getObjectMapper().readTree(output.toString());
        Assertions.assertNotNull(body);
        Assertions.assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), body.get("status").asInt());
        Assertions.assertEquals("Service Unavailable", body.get("title").asText());
        Assertions.assertEquals(authenticationException.getMessage(), body.get("detail").asText());
        Assertions.assertEquals("https://hmcts.gov.uk/problems/downstream-service-unavailable", body.get("type").asText());
        Assertions.assertEquals("op-789", body.get("operation_id").asText());
        Assertions.assertEquals(false, body.get("retriable").asBoolean());
    }
}
