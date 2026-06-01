package uk.gov.hmcts.opal.common.user.authentication.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.opal.common.controllers.advice.OpalProblemDetailFactory;
import uk.gov.hmcts.opal.common.logging.LogUtil;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.util.Map;

@Slf4j(topic = "opal.CommonGlobalExceptionHandler")
@ControllerAdvice
@RequiredArgsConstructor
public class CommonGlobalExceptionHandler {

    public static final String UNKNOWN = "Unknown";
    public static final String EVENT_NAME = "Authorisation Access Control";
    public static final String EVENT_ACTION_OUTCOME = "Failure";
    public static final String EVENT_OP_TYPE = "Authentication";

    private final UserStateClientService userStateClientService;
    private final SecurityEventLoggingService securityEventLoggingService;

    @ExceptionHandler({PermissionNotAllowedException.class})
    public ResponseEntity<ProblemDetail> handlePermissionNotAllowedException(PermissionNotAllowedException ex,
                                                                             HttpServletRequest request) {

        UserStateV2 userState = userStateClientService.getUserStateByAuthenticatedUser().orElse(null);
        String userId = userState != null ? String.valueOf(userState.getUserId()) : UNKNOWN;

        Short businessUnitId = ex.getBusinessUnitId();
        String message = ex.getMessage() != null ? ex.getMessage() : UNKNOWN;
        String resource = request.getPathInfo() != null ? request.getPathInfo() : UNKNOWN;

        Map<String, Object> eventData = Map.of(
            "UserIdentifier", userId,
            "Permission", message,
            "Resource", resource);

        securityEventLoggingService.logEvent(
            EVENT_NAME,
            EVENT_ACTION_OUTCOME,
            businessUnitId,
            EVENT_OP_TYPE,
            LogUtil.getRequestTimestamp(),
            eventData
        );

        ProblemDetail problemDetail = OpalProblemDetailFactory.createProblemDetail(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            "You do not have permission to access this resource",
            "forbidden",
            false,
            ex,
            log);

        return OpalProblemDetailFactory.responseWithProblemDetail(HttpStatus.FORBIDDEN, problemDetail);
    }
}
