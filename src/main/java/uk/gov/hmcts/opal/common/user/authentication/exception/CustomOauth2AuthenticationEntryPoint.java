package uk.gov.hmcts.opal.common.user.authentication.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.common.logging.LogUtil;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.ToJsonString;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import static uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService.AUTH_HEADER;

@Slf4j(topic = "opal.CustomOauth2AuthenticationEntryPoint")
@Component
@RequiredArgsConstructor
public class CustomOauth2AuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String EVENT_NAME = "Authorisation Access Control";
    public static final String EVENT_ACTION_OUTCOME = "Failure";
    public static final String EVENT_OP_TYPE = "Authentication";

    private final SecurityEventLoggingService securityEventLoggingService;
    private final AccessTokenService tokenService;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException ex) throws IOException {

        String authorization = request.getHeader(AUTH_HEADER);
        String oid = extractOid(authorization);
        String userIdentifier = oid != null ? oid : request.getRemoteAddr();

        Map<String, Object> eventData = Map.of(
            "UserIdentifier", userIdentifier,
            "Details", "Invalid access token",
            "Resource", request.getRequestURI());

        securityEventLoggingService.logEvent(
            EVENT_NAME,
            EVENT_ACTION_OUTCOME,
            null,
            EVENT_OP_TYPE,
            LogUtil.getRequestTimestamp(),
            eventData
        );

        String opalOperationId = LogUtil.getOrCreateOpalOperationId();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "You are not authorized to access this resource");
        problemDetail.setTitle("Unauthorized");
        problemDetail.setType(URI.create("https://hmcts.gov.uk/problems/unauthorized"));
        problemDetail.setInstance(URI.create("https://hmcts.gov.uk/problems/instance/" + opalOperationId));
        problemDetail.setProperty("operation_id", opalOperationId);
        problemDetail.setProperty("retriable", false);
        String problemDetailJson = ToJsonString.OBJECT_MAPPER.writeValueAsString(problemDetail);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        try (PrintWriter writer = response.getWriter()) {
            writer.write(problemDetailJson);
        }
    }

    private String extractOid(String authorization) {
        try {
            return Optional.ofNullable(authorization)
                .map(tokenService::extractOid)
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
