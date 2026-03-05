package uk.gov.hmcts.opal.common.user.authentication.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.opal.common.logging.LogUtil;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "opal.CommonGlobalExceptionHandler")
@ControllerAdvice
@RequiredArgsConstructor
public class CommonGlobalExceptionHandler {

    public static final String UNKNOWN = "'Unknown'";
    public static final String EVENT_NAME = "Authorisation Access Control";
    public static final String EVENT_ACTION_OUTCOME = "Failure";
    public static final String EVENT_OP_TYPE = "Authentication";

    private final UserStateClientService userStateClientService;
    private final SecurityEventLoggingService securityEventLoggingService;

    @ExceptionHandler({PermissionNotAllowedException.class})
    public ResponseEntity<ProblemDetail> handlePermissionNotAllowedException(PermissionNotAllowedException ex,
                                                                             HttpServletRequest request) {

        UserState userState = userStateClientService.getUserStateByAuthenticatedUser().orElse(null);
        String userId = userState != null ? String.valueOf(userState.getUserId()) : UNKNOWN;

        List<String> permissionNames = Arrays.stream(ex.getPermission())
            .map(PermissionDescriptor::toString)
            .toList();
        String permissionsDesc = Strings.join(permissionNames, ':');

        Short businessUnitId = null;
        BusinessUnitUser businessUnitUser = ex.getBusinessUnitUser();
        if (businessUnitUser != null) {
            businessUnitId = businessUnitUser.getBusinessUnitId();
        }

        String resource = request.getPathInfo() != null ? request.getPathInfo() : UNKNOWN;

        Map<String, Object> eventData = Map.of(
            "UserIdentifier", userId,
            "Permission", permissionsDesc,
            "Resource", resource);

        securityEventLoggingService.logEvent(
            EVENT_NAME,
            EVENT_ACTION_OUTCOME,
            businessUnitId,
            EVENT_OP_TYPE,
            LogUtil.getRequestTimestamp(),
            eventData
        );

        String opalOperationId = LogUtil.getOrCreateOpalOperationId();
        log.error("Error ID {}:", opalOperationId, ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "You do not have permission to access this resource");
        problemDetail.setTitle("Forbidden");
        problemDetail.setType(URI.create("https://hmcts.gov.uk/problems/forbidden"));
        problemDetail.setInstance(URI.create("https://hmcts.gov.uk/problems/instance/" + opalOperationId));
        problemDetail.setProperty("operation_id", opalOperationId);
        problemDetail.setProperty("retriable", false);

        BodyBuilder builder = ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        return builder.body(problemDetail);
    }
}
