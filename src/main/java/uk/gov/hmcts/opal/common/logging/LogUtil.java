package uk.gov.hmcts.opal.common.logging;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.BaseTelemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j(topic = "opal.LogUtil")
public final class LogUtil {

    private LogUtil() {

    }

    // Common error messages
    // =====================
    // These can be refactored to an error message mapping module for simplified
    // error messages to be returned to clients

    public static final String ERRMSG_STORED_PROC_FAILURE = "Stored Procedure Failure.";

    public static String getOrCreateOpalOperationId() {
        return getOperationContext()
            .map(OperationContext::getId)
            .orElseGet(LogUtil::createOpalOperation);
    }

    public static String createOpalOperation() {
        String operationId = UUID.randomUUID().toString().replace("-", "");
        log.debug(":createOpalOperation: Operation ID: {}", operationId);

        //Update the operation context if it exists, otherwise use MDC
        Optional<OperationContext> operationContextOpt = getOperationContext();
        if (operationContextOpt.isPresent()) {
            OperationContext operationContext = operationContextOpt.get();
            operationContext.setId(operationId);
            return operationId;
        } else {
            MDC.put("opal-operation-id", operationId);
        }
        return operationId;
    }

    private static Optional<OperationContext> getOperationContext() {
        return Optional.ofNullable(ThreadContext.getRequestTelemetryContext())
            .map(RequestTelemetryContext::getHttpRequestTelemetry)
            .map(BaseTelemetry::getContext)
            .map(TelemetryContext::getOperation);
    }

    public static String getIpAddress() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            log.debug(":getIpAddress: No request attributes available.");
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String headerIp = request.getHeader("X-User-IP");

        if (headerIp != null && !headerIp.isBlank()) {
            return headerIp;
        }

        log.debug(":getIpAddress: X-User-IP header missing or blank; falling back to authentication details.");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            log.debug(":getIpAddress: No authentication available.");
            return null;
        }

        Object details = auth.getDetails();
        if (details instanceof WebAuthenticationDetails webAuth) {
            return webAuth.getRemoteAddress();
        }

        log.debug(":getIpAddress: Authentication details not WebAuthenticationDetails.");

        return null;
    }

    public static OffsetDateTime getCurrentDateTime(Clock clock) {
        return OffsetDateTime.now(clock);
    }

    public static Optional<HttpServletRequest> getCurrentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra
            ? Optional.of(sra.getRequest())
            : Optional.empty();
    }

    public static Optional<Instant> getRequestInstant() {
        return getCurrentRequest()
            .map(req -> (Instant) req.getAttribute(RequestInstantFilter.REQUEST_RECEIVED_INSTANT));
    }

    public static LocalDateTime getRequestTimestamp() {
        return getRequestInstant()
            .map(instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))
            .orElse(LocalDateTime.now());
    }
}
