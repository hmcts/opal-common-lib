package uk.gov.hmcts.opal.common.spring.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.Assert;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.util.Collection;

@Slf4j
public class OpalJwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;
    private final JwtDecoder jwtDecoder;
    private final UserStateClientService userStateClientService;
    private final Domain domain;


    public OpalJwtAuthenticationProvider(JwtDecoder jwtDecoder, UserStateClientService userStateClientService,
                                         JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
                                         Domain domain) {
        Assert.notNull(jwtDecoder, "jwtDecoder cannot be null");
        this.jwtDecoder = jwtDecoder;
        this.userStateClientService = userStateClientService;
        this.jwtGrantedAuthoritiesConverter = jwtGrantedAuthoritiesConverter;
        this.domain = domain;
    }

    /**
     * Decode and validate the
     * <a href="https://tools.ietf.org/html/rfc6750#section-1.2" target="_blank">Bearer
     * Token</a>.
     *
     * @param authentication the authentication request object.
     * @return A successful authentication
     * @throws AuthenticationException if authentication failed for some reason
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        BearerTokenAuthenticationToken bearer = (BearerTokenAuthenticationToken) authentication;
        Jwt jwt;
        try {
            jwt = this.jwtDecoder.decode(bearer.getToken());
        } catch (BadJwtException failed) {
            log.debug("Failed to authenticate since the JWT was invalid");
            throw new InvalidBearerTokenException(failed.getMessage(), failed);
        } catch (JwtException failed) {
            throw new AuthenticationServiceException(failed.getMessage(), failed);
        }
        Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

        UserStateV2 userState = userStateClientService.getUserStateByAuthenticationToken(jwt)
            .orElseThrow(() -> new InvalidBearerTokenException("User state not found for authenticated user"));

        return new OpalJwtAuthenticationToken(userState, domain, jwt, authorities, bearer.getDetails());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
