package uk.gov.hmcts.opal.common.user.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

import java.util.Optional;

public final class SecurityUtil {
    private SecurityUtil() {

    }

    public static OpalJwtAuthenticationToken getAuthenticationToken() {
        Authentication authentication = Optional.of(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .orElse(null);
        return getAuthenticationToken(authentication);
    }

    public static OpalJwtAuthenticationToken getAuthenticationToken(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Authentication token must be provided");
        }
        if (authentication instanceof OpalJwtAuthenticationToken opalJwtAuthenticationToken) {
            return opalJwtAuthenticationToken;
        }
        throw new IllegalStateException("Authentication token is not of type OpalJwtAuthenticationToken");
    }

    public static UserState getUserState() {
        return getAuthenticationToken().getUserState();
    }
}
