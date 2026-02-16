package uk.gov.hmcts.opal.common.logging;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public interface SecurityEventLoggingService {
    void logEvent(
        @NotBlank final String eventName, @NotBlank final String actionOutcome,
        final Short buId, @NotBlank String opType,
        final LocalDateTime opTimestamp, final Map<String, Object> eventData);
}
