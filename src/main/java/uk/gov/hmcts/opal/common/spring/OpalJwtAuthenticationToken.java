package uk.gov.hmcts.opal.common.spring;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

import java.util.Collection;

@Getter
public class OpalJwtAuthenticationToken extends JwtAuthenticationToken {

    private final UserState userState;


    public OpalJwtAuthenticationToken(UserState userState, Jwt jwt,
        Collection<? extends GrantedAuthority> authorities) {
        super(jwt, authorities, jwt.getClaimAsString(JwtClaimNames.SUB));
        this.userState = userState;
    }
}
