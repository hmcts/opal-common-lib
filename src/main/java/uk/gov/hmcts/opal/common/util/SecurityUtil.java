package uk.gov.hmcts.opal.common.util;

import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.common.exceptions.standard.UnauthorizedException;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;

public class SecurityUtil {

    public static OpalJwtAuthenticationToken getOpalJwtAuthenticationTokenForCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof OpalJwtAuthenticationToken opalJwtAuthenticationToken) {
            return opalJwtAuthenticationToken;
        }
        throw new UnauthorizedException(
            "Unauthorised",
            "Current user is not authenticated with OpalJwtAuthenticationToken"
        );
    }
}
