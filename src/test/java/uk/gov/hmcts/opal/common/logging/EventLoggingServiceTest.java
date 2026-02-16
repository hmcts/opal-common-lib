package uk.gov.hmcts.opal.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import uk.gov.hmcts.opal.common.config.OpalCommonConfiguration;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLoggingService.class)
@Import(MethodValidationPostProcessor.class)
class EventLoggingServiceTest {

    @MockitoBean
    private OpalCommonConfiguration config;

    @MockitoBean
    private ClockService clock;

    @MockitoBean
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<ILoggingEvent> captor;

    @Autowired
    private SecurityEventLoggingService service;

    @BeforeEach
    void setupEach() {
        Logger logger = (Logger) LoggerFactory.getLogger("opal.EventLoggingService");
        logger.addAppender(mockAppender);
    }

    @Test
    void logEvent_nullsNotAllowed() {
        // Act
        ConstraintViolationException cve = assertThrows(
            ConstraintViolationException.class,
            () -> service.logEvent(null, null, null, null, null, null)
        );

        // Assert
        assertBlanks(cve);
    }

    @Test
    void logEvent_emptyStringsNotAllowed() {
        // Act
        ConstraintViolationException cve = assertThrows(
            ConstraintViolationException.class,
            () -> service.logEvent("","",null,"",null,null)
        );

        // Assert
        assertBlanks(cve);
    }

    @Test
    void logEvent_whitespaceStringsNotAllowed() {
        // Act
        ConstraintViolationException cve = assertThrows(
            ConstraintViolationException.class,
            () -> service.logEvent("  \t"," \n ",null,"\n\t",null,null)
        );

        // Assert
        assertBlanks(cve);
    }

    private void assertBlanks(ConstraintViolationException cve) {
        Set<ConstraintViolation<?>> violations = cve.getConstraintViolations();
        assertEquals(3, violations.size(), "Expected 3 parameter violations for not-blank fields");
        assertEquals(3, violations.stream()
            .map(ConstraintViolation::getMessage)
            .filter("must not be blank"::equals)
            .count(), "Expected all violations to be 'must not be blank'");
        List<String> paths = violations.stream()
            .map(ConstraintViolation::getPropertyPath)
            .map(Path::toString)
            .toList();

        assertTrue(paths.contains("logEvent.eventName"), "Parameter 'eventName' should be tested for 'blank'");
        assertTrue(paths.contains("logEvent.actionOutcome"), "Parameter 'actionOutcome' should be tested for 'blank'");
        assertTrue(paths.contains("logEvent.opType"), "Parameter 'opType' should be tested for 'blank'");
    }

    @Test
    void logEvent_minimalParams() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        when(config.getDomain()).thenReturn("TestDomain");
        when(clock.now()).thenReturn(now);
        String opId = UUID.randomUUID().toString().replace("-", "");
        ThreadContext.setRequestTelemetryContext(mockRequestTelemetryContext(opId));

        // Act
        service.logEvent("TestEvent", "Success", null, "TestType",null, null);

        // Assert
        verify(mockAppender, atLeastOnce()).doAppend(captor.capture());
        List<ILoggingEvent> events = captor.getAllValues();
        String nowText = now.toString();
        String compareTo = "TestEvent,Success," + nowText + ",TestDomain,," + opId + ",TestType," + nowText + ",";
        events.stream().findFirst().ifPresentOrElse(
            e -> assertEquals(compareTo, e.getFormattedMessage()), () -> fail("No Log event captured."));
    }

    @Test
    void logEvent_populatedParams() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        when(config.getDomain()).thenReturn("TestDomain");
        when(clock.now()).thenReturn(now);
        String opId = UUID.randomUUID().toString().replace("-", "");
        ThreadContext.setRequestTelemetryContext(mockRequestTelemetryContext(opId));
        Short buid = (short)7;
        LocalDateTime opTimestamp = LocalDateTime.now();
        Map<String, Object> data = new TreeMap<>(
            Map.of("key", "value", "object", new BigDecimal(3), "when", opTimestamp));
        String dataText = data.entrySet().stream().map(e -> e.getKey()
            + "=" + e.getValue()).collect(Collectors.joining(","));

        // Act
        service.logEvent("TestEvent", "Success", buid, "TestType",opTimestamp, data);

        // Assert
        verify(mockAppender, atLeastOnce()).doAppend(captor.capture());
        List<ILoggingEvent> events = captor.getAllValues();
        String nowText = now.toString();
        String compareTo = "TestEvent,Success," + nowText + ",TestDomain," + buid.toString()
            + "," + opId + ",TestType," + opTimestamp + "," + dataText;
        events.stream().findFirst().ifPresentOrElse(
            e -> assertEquals(compareTo, e.getFormattedMessage()), () -> fail("No Log event captured."));
    }

    private RequestTelemetryContext mockRequestTelemetryContext(String id) {
        RequestTelemetryContext rtc = mock(RequestTelemetryContext.class);
        ThreadContext.setRequestTelemetryContext(rtc);
        RequestTelemetry rt = mock(RequestTelemetry.class);
        when(rtc.getHttpRequestTelemetry()).thenReturn(rt);
        TelemetryContext tc = mock(TelemetryContext.class);
        when(rt.getContext()).thenReturn(tc);
        OperationContext oc = mock(OperationContext.class);
        when(tc.getOperation()).thenReturn(oc);
        when(oc.getId()).thenReturn(id);
        return rtc;
    }
}
