package uk.gov.hmcts.opal.common.spring.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.common.config.OpalCommonConfiguration;
import uk.gov.hmcts.opal.common.logging.SecurityEventLogging.ActionOutcome;
import uk.gov.hmcts.opal.common.logging.SecurityEventLogging.EventDataKey;
import uk.gov.hmcts.opal.common.logging.SecurityEventLogging.EventName;
import uk.gov.hmcts.opal.common.logging.SecurityEventLogging.EventOpType;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;

/**
 * Logs successful system-user authentication and existing authentication failures. User-Agent is a claim, never an
 * authenticated service identity.
 */
@Component
@AllArgsConstructor
public class AuthenticationLogging {

    private final SecurityEventLoggingService securityEventLoggingService;
    private final OpalCommonConfiguration commonConfiguration;


    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }

    public void logSuccess(
        OpalJwtAuthenticationToken token,
        HttpServletRequest request
    ) {
        if (token.getUserState().isSystemUser()) {
            securityEventLoggingService.logEvent(
                EventName.SERVICE_TO_SERVICE_AUTH,
                ActionOutcome.PASS,
                null,
                EventOpType.AUTHENTICATION,
                null, // the logging service populates this internally
                Map.of(
                    EventDataKey.AUTHENTICATING_SERVICE, getUserAgent(request),
                    EventDataKey.TARGET_SERVICE, commonConfiguration.getDomain())
            );
        }
    }
}
