package uk.gov.hmcts.opal.common.spring.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
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
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

import java.util.Collection;

@Slf4j
public class OpalJwtAuthenticationProvider implements AuthenticationProvider {

    private final Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter =
        new JwtGrantedAuthoritiesConverter();
    private final JwtDecoder jwtDecoder;
    private final UserStateClientService userStateClientService;


    public OpalJwtAuthenticationProvider(JwtDecoder jwtDecoder, UserStateClientService userStateClientService) {
        Assert.notNull(jwtDecoder, "jwtDecoder cannot be null");
        this.jwtDecoder = jwtDecoder;
        this.userStateClientService = userStateClientService;
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
        Jwt jwt = getJwt(bearer);
        UserState userState = userStateClientService.getUserStateByAuthenticationToken(jwt)
            .orElseThrow(() -> new InvalidBearerTokenException("User state not found for authenticated user"));

        Collection<GrantedAuthority> authorities = this.jwtGrantedAuthoritiesConverter.convert(jwt);

        OpalJwtAuthenticationToken token = new OpalJwtAuthenticationToken(userState, jwt, authorities);

        if (token.getDetails() == null) {
            token.setDetails(bearer.getDetails());
        }
        log.debug("Authenticated token");
        return token;
    }


    private Jwt getJwt(BearerTokenAuthenticationToken bearer) {
        try {
            return this.jwtDecoder.decode(bearer.getToken());
        } catch (BadJwtException failed) {
            log.debug("Failed to authenticate since the JWT was invalid");
            throw new InvalidBearerTokenException(failed.getMessage(), failed);
        } catch (JwtException failed) {
            throw new AuthenticationServiceException(failed.getMessage(), failed);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
