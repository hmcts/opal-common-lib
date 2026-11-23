package uk.gov.hmcts.opal.common.spring.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.opal.common.config.OpalCommonConfiguration;
import uk.gov.hmcts.opal.common.logging.SecurityEventLogging.ActionOutcome;
import uk.gov.hmcts.opal.common.logging.SecurityEventLogging.EventDataKey;
import uk.gov.hmcts.opal.common.logging.SecurityEventLogging.EventName;
import uk.gov.hmcts.opal.common.logging.SecurityEventLogging.EventOpType;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

@ExtendWith(MockitoExtension.class)
class AuthenticationLoggingTest {

    @Mock
    private SecurityEventLoggingService eventLoggingService;

    private OpalCommonConfiguration configuration;
    private AuthenticationLogging authenticationLogging;

    @BeforeEach
    void setUp() {
        configuration = new OpalCommonConfiguration();
        configuration.setDomain("opal-fines-service");
        authenticationLogging = new AuthenticationLogging(eventLoggingService, configuration);
    }

    @ParameterizedTest
    @ValueSource(strings = {"opal-fines-service", "opal-file-handler-service"})
    void logSuccess_recordsSystemUser(String domain) {
        configuration.setDomain(domain);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "calling-service");
        request.addHeader("Authorization", "Bearer secret-token");

        authenticationLogging.logSuccess(createToken(true), request);

        verify(eventLoggingService).logEvent(
            EventName.SERVICE_TO_SERVICE_AUTH, ActionOutcome.PASS, null, EventOpType.AUTHENTICATION, null,
            Map.of(EventDataKey.AUTHENTICATING_SERVICE, "calling-service", EventDataKey.TARGET_SERVICE, domain));
        verifyNoMoreInteractions(eventLoggingService);
    }

    @Test
    void logSuccess_usesUnknownForMissingUserAgent() {
        authenticationLogging.logSuccess(createToken(true), new MockHttpServletRequest());

        verify(eventLoggingService).logEvent(
            EventName.SERVICE_TO_SERVICE_AUTH, ActionOutcome.PASS, null, EventOpType.AUTHENTICATION, null,
            Map.of(EventDataKey.AUTHENTICATING_SERVICE, "Unknown", EventDataKey.TARGET_SERVICE, "opal-fines-service"));
        verifyNoMoreInteractions(eventLoggingService);
    }

    @Test
    void logSuccess_ignoresStandardUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "calling-service");

        authenticationLogging.logSuccess(createToken(false), request);

        verifyNoInteractions(eventLoggingService);
    }

    private OpalJwtAuthenticationToken createToken(boolean systemUser) {
        UserStateV2 userState = UserStateV2.builder()
            .userId(1L)
            .username("test-user")
            .systemUser(systemUser)
            .build();
        Jwt jwt = Jwt.withTokenValue("jwt-not-for-logging")
            .header("alg", "RS256")
            .subject("test-user")
            .build();
        return new OpalJwtAuthenticationToken(userState, Domain.FINES, jwt, List.of(), null);
    }
}
