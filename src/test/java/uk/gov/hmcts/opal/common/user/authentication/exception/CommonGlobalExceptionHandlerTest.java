package uk.gov.hmcts.opal.common.user.authentication.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.opal.common.logging.LogUtil;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommonGlobalExceptionHandlerTest {

    @Mock
    UserStateClientService userStateClientService;

    @Mock
    SecurityEventLoggingService securityEventLoggingService;


    @InjectMocks
    CommonGlobalExceptionHandler handler;

    @Test
    void testHandlePermissionNotAllowedException() {

        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);

        LocalDateTime localDateTime = LocalDateTime.of(2025, 4, 1,  10, 0, 0, 0);
        String opalOperationId = "op-id";
        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getRequestTimestamp).thenReturn(localDateTime);
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn(opalOperationId);

            UserState userState = UserState.builder()
                .userId(123L)
                .userName("test-user")
                .businessUnitUser(Collections.emptySet())
                .build();

            Short businessUnitId = 42;

            PermissionNotAllowedException exception =
                new PermissionNotAllowedException(businessUnitId, TestPermission.READ_CASE);

            when(userStateClientService.getUserStateByAuthenticatedUser()).thenReturn(Optional.of(userState));
            when(request.getPathInfo()).thenReturn("/api/cases/1");

            // Act
            ResponseEntity<ProblemDetail> response = handler.handlePermissionNotAllowedException(exception, request);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
            ProblemDetail body = response.getBody();
            assertNotNull(body);
            assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
            assertEquals("Forbidden", body.getTitle());
            assertEquals("You do not have permission to access this resource", body.getDetail());
            assertEquals("https://hmcts.gov.uk/problems/forbidden", body.getType().toString());
            assertEquals("op-id", body.getProperties().get("operation_id"));
            assertEquals(false, body.getProperties().get("retriable"));

            verify(securityEventLoggingService).logEvent(
                eq("Authorisation Access Control"),
                eq("Failure"),
                eq((short) 42),
                eq("Authentication"),
                eq(localDateTime),
                argThat(eventData -> "123".equals(eventData.get("UserIdentifier"))
                    && "READ_CASE".equals(eventData.get("Permission"))
                    && "/api/cases/1".equals(eventData.get("Resource"))));

        }
    }

    private enum TestPermission implements PermissionDescriptor {
        READ_CASE;

        @Override
        public long getId() {
            return 1L;
        }

        @Override
        public String getDescription() {
            return "Read case";
        }
    }
}
