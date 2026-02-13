package uk.gov.hmcts.opal.common.spring.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpalJwtAuthenticationProviderTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private UserStateClientService userStateClientService;

    @Mock
    private JwtGrantedAuthoritiesConverter converter;

    @InjectMocks
    @Spy
    private OpalJwtAuthenticationProvider opalJwtAuthenticationProvider;


    @Test
    void supports_shouldReturnTrueIfTypeIsBearerTokenAuthenticationToken() {
        assertThat(opalJwtAuthenticationProvider.supports(BearerTokenAuthenticationToken.class))
            .isTrue();
    }

    @Test
    void supports_shouldReturnFalseIfTypeIsNotBearerTokenAuthenticationToken() {
        assertThat(opalJwtAuthenticationProvider.supports(Object.class))
            .isFalse();
    }

    @Test
    void getJwt_shouldReturnJwtWithValidToken() {
        Jwt jwt = mock(Jwt.class);
        String token = "my.token.value";

        BearerTokenAuthenticationToken authenticationToken = mock(BearerTokenAuthenticationToken.class);
        when(authenticationToken.getToken()).thenReturn(token);
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        assertThat(opalJwtAuthenticationProvider.getJwt(authenticationToken)).isEqualTo(jwt);

        verify(jwtDecoder).decode(token);
        verify(authenticationToken).getToken();
    }

    @Test
    void getJwt_shouldThrowErrorWhenBadToken() {
        String token = "my.token.value";

        BearerTokenAuthenticationToken authenticationToken = mock(BearerTokenAuthenticationToken.class);
        when(authenticationToken.getToken()).thenReturn(token);
        BadJwtException exception = mock(BadJwtException.class);
        when(exception.getMessage()).thenReturn("some-bad-jwt-error");

        when(jwtDecoder.decode(token)).thenThrow(exception);

        InvalidBearerTokenException actualException = assertThrows(InvalidBearerTokenException.class, () ->
            opalJwtAuthenticationProvider.getJwt(authenticationToken)
        );
        assertThat(actualException.getMessage())
            .isEqualTo("some-bad-jwt-error");
        assertThat(actualException.getCause())
            .isEqualTo(exception);
    }

    @Test
    void getJwt_shouldThrowErrorForAnyNonBadJwtException() {
        String token = "my.token.value";

        BearerTokenAuthenticationToken authenticationToken = mock(BearerTokenAuthenticationToken.class);
        when(authenticationToken.getToken()).thenReturn(token);
        JwtException exception = mock(JwtException.class);
        when(exception.getMessage()).thenReturn("some-jwt-error");

        when(jwtDecoder.decode(token)).thenThrow(exception);

        AuthenticationServiceException actualException = assertThrows(AuthenticationServiceException.class, () ->
            opalJwtAuthenticationProvider.getJwt(authenticationToken)
        );
        assertThat(actualException.getMessage())
            .isEqualTo("some-jwt-error");
        assertThat(actualException.getCause())
            .isEqualTo(exception);
    }

    @Test
    void authenticate_shouldErrorWhenUserStateNotFound() {
        BearerTokenAuthenticationToken authenticationToken = mock(BearerTokenAuthenticationToken.class);
        Jwt jwt = mock(Jwt.class);
        doReturn(jwt).when(opalJwtAuthenticationProvider).getJwt(authenticationToken);

        when(userStateClientService.getUserStateByAuthenticationToken(jwt))
            .thenReturn(Optional.empty());
        InvalidBearerTokenException exception = assertThrows(InvalidBearerTokenException.class, () ->
            opalJwtAuthenticationProvider.authenticate(authenticationToken)
        );
        assertThat(exception.getMessage())
            .isEqualTo("User state not found for authenticated user");
        verify(userStateClientService).getUserStateByAuthenticationToken(jwt);
        verify(opalJwtAuthenticationProvider).getJwt(authenticationToken);
    }

    @Test
    void authenticate_shouldReturnAuthenticationWhenUserStateFound() {
        BearerTokenAuthenticationToken authenticationToken = mock(BearerTokenAuthenticationToken.class);
        Jwt jwt = mock(Jwt.class);
        UserState userState = mock(UserState.class);
        when(authenticationToken.getDetails()).thenReturn("some detail");

        doReturn(jwt).when(opalJwtAuthenticationProvider).getJwt(authenticationToken);

        when(userStateClientService.getUserStateByAuthenticationToken(jwt))
            .thenReturn(Optional.of(userState));

        Collection<GrantedAuthority> authorities = Set.of(
            mock(GrantedAuthority.class), mock(GrantedAuthority.class), mock(GrantedAuthority.class)
        );

        when(converter.convert(jwt)).thenReturn(authorities);

        Authentication authentication = opalJwtAuthenticationProvider.authenticate(authenticationToken);
        assertThat(authentication)
            .isInstanceOf(OpalJwtAuthenticationToken.class);
        OpalJwtAuthenticationToken opalJwtAuthenticationToken = (OpalJwtAuthenticationToken) authentication;

        assertThat(opalJwtAuthenticationToken.getUserState())
            .isEqualTo(userState);

        assertThat(opalJwtAuthenticationToken.getAuthorities())
            .containsAll(authorities);
        assertThat(opalJwtAuthenticationToken.getDetails())
            .isEqualTo("some detail");

        verify(userStateClientService).getUserStateByAuthenticationToken(jwt);
        verify(opalJwtAuthenticationProvider).getJwt(authenticationToken);
        verify(converter).convert(jwt);

    }
}
