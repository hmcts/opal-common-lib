package uk.gov.hmcts.opal.common.user.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

public final class SecurityUtil {

    public static OpalJwtAuthenticationToken getAuthenticationToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return getAuthenticationToken(authentication);
    }

    public static OpalJwtAuthenticationToken getAuthenticationToken(Authentication authentication) {
        if (authentication instanceof OpalJwtAuthenticationToken opalJwtAuthenticationToken) {
            return opalJwtAuthenticationToken;
        } else {
            throw new IllegalStateException("Authentication token is not of type OpalJwtAuthenticationToken");
        }
    }

    public static UserState getUserState() {
        return getAuthenticationToken().getUserState();
    }
}
