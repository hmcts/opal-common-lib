package uk.gov.hmcts.opal.common.logging;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import uk.gov.hmcts.opal.common.config.OpalCommonConfiguration;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.EventLoggingService")
@Validated
public class EventLoggingService implements SecurityEventLoggingService {

    private final OpalCommonConfiguration commonConfig;
    private final ClockService clockService;

    @Override
    public void logEvent(
        @NotBlank final String eventName, @NotBlank final String actionOutcome,
        final Short buId, @NotBlank String opType,
        final LocalDateTime opTimestamp, final Map<String, Object> eventData) {

        String businessUnit = buId == null ? "" : String.valueOf(buId); // Use empty string if buId is null.
        String logTimestamp = clockService.now().toString();

        String operationTimestamp = opTimestamp == null ? logTimestamp : opTimestamp.toString();

        String domain = commonConfig.getDomain();
        String operation = LogUtil.getOrCreateOpalOperationId();

        String data = eventData == null ? "" : eventData.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(","));

        String logMessage = String.join(",", eventName, actionOutcome, logTimestamp, domain,
                                        businessUnit, operation, opType, operationTimestamp, data);

        log.info(logMessage);
    }
}
