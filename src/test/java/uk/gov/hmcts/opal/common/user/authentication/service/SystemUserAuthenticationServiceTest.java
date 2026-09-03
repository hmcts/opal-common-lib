package uk.gov.hmcts.opal.common.user.authentication.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.common.exceptions.standard.UnauthorizedException;
import uk.gov.hmcts.opal.common.config.OpalCommonConfiguration;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.client.AzureActiveDirectoryClient;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.AzureToken;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemUserAuthenticationServiceTest {

    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final String SCOPE = "https://example/.default";
    private static final String GRANT_TYPE = "client_credentials";

    @Mock
    private OpalCommonConfiguration opalCommonConfiguration;

    @Mock
    private OpalCommonConfiguration.SystemUsers systemUsers;

    @Mock
    private AzureActiveDirectoryClient azureActiveDirectoryClient;

    @InjectMocks
    private SystemUserAuthenticationService systemUserAuthenticationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSystemUserAuthenticationToken_whenSystemUserExists_returnsAccessToken() {
        OpalCommonConfiguration.SystemUser configuredSystemUser = buildSystemUser();
        AzureToken azureToken = new AzureToken();
        azureToken.setAccessToken("access-token");
        stubSystemUsers(Map.of(SystemUserEnum.OPAL_SYSTEM_USER.getConfigKey(), configuredSystemUser));

        when(azureActiveDirectoryClient.getSystemUser(CLIENT_ID, CLIENT_SECRET, SCOPE, GRANT_TYPE))
            .thenReturn(azureToken);

        String accessToken = systemUserAuthenticationService
            .getSystemUserAuthenticationToken(SystemUserEnum.OPAL_SYSTEM_USER);

        assertThat(accessToken).isEqualTo("access-token");
        verify(azureActiveDirectoryClient).getSystemUser(CLIENT_ID, CLIENT_SECRET, SCOPE, GRANT_TYPE);
    }

    @Test
    void getClientId_whenSystemUserExists_returnsClientId() {
        OpalCommonConfiguration.SystemUser configuredSystemUser = buildSystemUser();
        stubSystemUsers(Map.of(SystemUserEnum.OPAL_SYSTEM_USER.getConfigKey(), configuredSystemUser));

        String clientId = systemUserAuthenticationService.getClientId(SystemUserEnum.OPAL_SYSTEM_USER);

        assertThat(clientId).isEqualTo(CLIENT_ID);
        verifyNoInteractions(azureActiveDirectoryClient);
    }

    @Test
    void getSystemUserAuthenticationToken_whenSystemUserMissing_throwsUnauthorizedException() {
        stubSystemUsers(Map.of());

        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> systemUserAuthenticationService.getSystemUserAuthenticationToken(SystemUserEnum.OPAL_SYSTEM_USER)
        );

        assertThat(exception.getDetail()).isEqualTo("System user not found for enum: opal-system-user");
        verifyNoInteractions(azureActiveDirectoryClient);
    }

    @Test
    void getClientId_whenSystemUserMissing_throwsUnauthorizedException() {
        stubSystemUsers(Map.of());

        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> systemUserAuthenticationService.getClientId(SystemUserEnum.OPAL_SYSTEM_USER)
        );

        assertThat(exception.getDetail()).isEqualTo("System user not found for enum: opal-system-user");
        verifyNoInteractions(azureActiveDirectoryClient);
    }

    @Test
    void getCurrentSystemUser_whenAppIdMatchesConfiguredSystemUser_returnsSystemUserEnum() {
        OpalCommonConfiguration.SystemUser configuredSystemUser = buildSystemUser();
        stubSystemUsers(Map.of(SystemUserEnum.OPAL_SYSTEM_USER.getConfigKey(), configuredSystemUser));
        setCurrentAuthenticatedSystemUser(CLIENT_ID);

        assertThat(systemUserAuthenticationService.getCurrentSystemUser())
            .contains(SystemUserEnum.OPAL_SYSTEM_USER);
        verifyNoInteractions(azureActiveDirectoryClient);
    }

    @Test
    void getCurrentSystemUser_whenAppIdClaimIsMissing_returnsEmpty() {
        setCurrentAuthenticatedSystemUser(null);

        assertThat(systemUserAuthenticationService.getCurrentSystemUser()).isEmpty();
        verifyNoInteractions(azureActiveDirectoryClient);
    }

    @Test
    void getCurrentSystemUser_whenAppIdDoesNotMatchAnyConfiguredSystemUser_returnsEmpty() {
        OpalCommonConfiguration.SystemUser configuredSystemUser = buildSystemUser();
        stubSystemUsers(Map.of(SystemUserEnum.OPAL_SYSTEM_USER.getConfigKey(), configuredSystemUser));
        setCurrentAuthenticatedSystemUser("other-client-id");

        assertThat(systemUserAuthenticationService.getCurrentSystemUser()).isEmpty();
        verifyNoInteractions(azureActiveDirectoryClient);
    }

    @Test
    void getCurrentSystemUser_whenConfigKeyHasNoMatchingEnum_returnsEmpty() {
        OpalCommonConfiguration.SystemUser configuredSystemUser = buildSystemUser();
        stubSystemUsers(Map.of("unknown-system-user", configuredSystemUser));
        setCurrentAuthenticatedSystemUser(CLIENT_ID);

        assertThat(systemUserAuthenticationService.getCurrentSystemUser()).isEmpty();
        verifyNoInteractions(azureActiveDirectoryClient);
    }

    @Test
    void getCurrentSystemUser_whenCurrentAuthenticationIsMissing_throwsUnauthorizedException() {
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> systemUserAuthenticationService.getCurrentSystemUser()
        );

        assertThat(exception.getDetail())
            .isEqualTo("Current user is not authenticated with OpalJwtAuthenticationToken");
        verifyNoInteractions(azureActiveDirectoryClient);
    }

    private void stubSystemUsers(Map<String, OpalCommonConfiguration.SystemUser> users) {
        when(opalCommonConfiguration.getSystemUsers()).thenReturn(systemUsers);
        when(systemUsers.getUsers()).thenReturn(users);
    }

    private OpalCommonConfiguration.SystemUser buildSystemUser() {
        OpalCommonConfiguration.SystemUser systemUser = new OpalCommonConfiguration.SystemUser();
        systemUser.setClientId(CLIENT_ID);
        systemUser.setClientSecret(CLIENT_SECRET);
        systemUser.setScope(SCOPE);
        systemUser.setGrantType(GRANT_TYPE);
        return systemUser;
    }

    private void setCurrentAuthenticatedSystemUser(String appId) {
        OpalJwtAuthenticationToken opalJwtAuthenticationToken = mock(OpalJwtAuthenticationToken.class);
        Jwt jwt = mock(Jwt.class);

        when(opalJwtAuthenticationToken.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("appid")).thenReturn(appId);

        SecurityContextHolder.getContext().setAuthentication(opalJwtAuthenticationToken);
    }
}

