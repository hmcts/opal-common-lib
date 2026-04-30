package uk.gov.hmcts.opal.common.spring.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpalJwtAuthenticationProviderTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private UserStateClientService userStateClientService;

    @Mock
    private JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;

    @Mock
    private Jwt jwt;

    OpalJwtAuthenticationProvider finesAuthProvider;

    @BeforeEach
    void setUp() {
        finesAuthProvider = createProvider(Domain.FINES);
    }

    @Test
    void authenticateShouldReturnOpalJwtAuthenticationTokenWhenJwtAndUserStateAreValid() {
        // Arrange
        String rawToken = "valid-token";
        BearerTokenAuthenticationToken bearerToken = new BearerTokenAuthenticationToken(rawToken);
        Object details = new Object();
        bearerToken.setDetails(details);
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
        List<GrantedAuthority> authorities = List.of(authority);
        UserStateV2 userState = createUserState(Domain.FINES);
        when(jwtDecoder.decode(rawToken)).thenReturn(jwt);
        when(jwtGrantedAuthoritiesConverter.convert(jwt)).thenReturn(authorities);
        when(userStateClientService.getUserStateByAuthenticationToken(jwt)).thenReturn(Optional.of(userState));
        when(jwt.getClaimAsString(JwtClaimNames.SUB)).thenReturn("test-sub");

        // Act
        Authentication authentication = finesAuthProvider.authenticate(bearerToken);

        //Assert
        OpalJwtAuthenticationToken opalAuthentication = assertInstanceOf(
            OpalJwtAuthenticationToken.class,
            authentication
        );
        assertEquals(userState.getUserId(), opalAuthentication.getUserId());
        assertEquals(userState.getUsername(), opalAuthentication.getUsername());
        assertEquals(authorities, opalAuthentication.getAuthorities());
        assertSame(details, opalAuthentication.getDetails());
        verify(jwtDecoder).decode(rawToken);
        verify(jwtGrantedAuthoritiesConverter).convert(jwt);
        verify(userStateClientService).getUserStateByAuthenticationToken(jwt);
    }

    @Test
    void authenticateShouldThrowInvalidBearerTokenExceptionWhenJwtIsBad() {
        // Arrange
        String rawToken = "bad-token";
        BearerTokenAuthenticationToken bearerToken = new BearerTokenAuthenticationToken(rawToken);
        BadJwtException badJwtException = new BadJwtException("bad jwt");
        when(jwtDecoder.decode(rawToken)).thenThrow(badJwtException);

        // Act
        InvalidBearerTokenException exception = assertThrows(
            InvalidBearerTokenException.class,
            () -> finesAuthProvider.authenticate(bearerToken)
        );

        //Assert
        assertEquals("bad jwt", exception.getMessage());
        assertSame(badJwtException, exception.getCause());
        verify(jwtDecoder).decode(rawToken);
        verifyNoInteractions(jwtGrantedAuthoritiesConverter, userStateClientService);
    }

    @Test
    void authenticateShouldThrowAuthenticationServiceExceptionWhenJwtDecodeFails() {
        // Arrange
        String rawToken = "decode-failure-token";
        BearerTokenAuthenticationToken bearerToken = new BearerTokenAuthenticationToken(rawToken);
        JwtException jwtException = new JwtException("decode failed");
        when(jwtDecoder.decode(rawToken)).thenThrow(jwtException);

        // Act
        AuthenticationServiceException exception = assertThrows(
            AuthenticationServiceException.class,
            () -> finesAuthProvider.authenticate(bearerToken)
        );

        //Assert
        assertEquals("decode failed", exception.getMessage());
        assertSame(jwtException, exception.getCause());
        verify(jwtDecoder).decode(rawToken);
        verifyNoInteractions(jwtGrantedAuthoritiesConverter, userStateClientService);
    }

    @Test
    void authenticateShouldThrowInvalidBearerTokenExceptionWhenUserStateIsMissing() {
        // Arrange
        String rawToken = "missing-user-state-token";
        BearerTokenAuthenticationToken bearerToken = new BearerTokenAuthenticationToken(rawToken);
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
        List<GrantedAuthority> authorities = List.of(authority);
        when(jwtDecoder.decode(rawToken)).thenReturn(jwt);
        when(jwtGrantedAuthoritiesConverter.convert(jwt)).thenReturn(authorities);
        when(userStateClientService.getUserStateByAuthenticationToken(jwt)).thenReturn(Optional.empty());

        // Act
        InvalidBearerTokenException exception = assertThrows(
            InvalidBearerTokenException.class,
            () -> finesAuthProvider.authenticate(bearerToken)
        );

        //Assert
        assertEquals("User state not found for authenticated user", exception.getMessage());
        verify(jwtDecoder).decode(rawToken);
        verify(jwtGrantedAuthoritiesConverter).convert(jwt);
        verify(userStateClientService).getUserStateByAuthenticationToken(jwt);
    }

    @Test
    void supportsShouldReturnTrueForBearerTokenAuthenticationToken() {
        assertTrue(finesAuthProvider.supports(BearerTokenAuthenticationToken.class));
    }

    @Test
    void supportsShouldReturnFalseForOtherAuthenticationTypes() {
        assertFalse(finesAuthProvider.supports(Authentication.class));
    }

    @Test
    void constructorShouldThrowIllegalArgumentExceptionWhenJwtDecoderIsNull() {
        // Arrange
        UserStateClientService localUserStateClientService = userStateClientService;
        JwtGrantedAuthoritiesConverter localAuthoritiesConverter = jwtGrantedAuthoritiesConverter;

        // Act
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new OpalJwtAuthenticationProvider(
                null,
                localUserStateClientService,
                localAuthoritiesConverter,
                Domain.FINES
            )
        );

        //Assert
        assertEquals("jwtDecoder cannot be null", exception.getMessage());
    }

    private OpalJwtAuthenticationProvider createProvider(Domain domain) {
        return new OpalJwtAuthenticationProvider(
            jwtDecoder,
            userStateClientService,
            jwtGrantedAuthoritiesConverter,
            domain
        );
    }

    private UserStateV2 createUserState(Domain domain) {
        Permission permA = Permission.builder()
            .permissionId(1L)
            .permissionName("PERM_A")
            .build();

        BusinessUnitUser businessUnitUser = BusinessUnitUser.builder()
            .businessUnitUserId("bu-user-1")
            .businessUnitId((short) 101)
            .permissions(Set.of(permA))
            .build();

        DomainBusinessUnitUsers domainBusinessUnitUsers = DomainBusinessUnitUsers.builder()
            .businessUnitUsers(List.of(businessUnitUser))
            .build();

        return UserStateV2.builder()
            .userId(10L)
            .username("test.user")
            .domains(Map.of(domain, domainBusinessUnitUsers))
            .build();
    }
}
