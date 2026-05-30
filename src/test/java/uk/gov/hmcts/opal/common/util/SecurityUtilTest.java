package uk.gov.hmcts.opal.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.common.exceptions.standard.UnauthorizedException;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SecurityUtilTest {


    @Test
    void getOpalJwtAuthenticationTokenForCurrentUser_hasAuthButNotCorrectType_shouldError() {
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UnauthorizedException exception =
            assertThrows(UnauthorizedException.class, SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);

        assertThat(exception.getTitle())
            .isEqualTo("Unauthorised");
        assertThat(exception.getDetail())
            .isEqualTo("Current user is not authenticated with OpalJwtAuthenticationToken");
    }

    @Test
    void getOpalJwtAuthenticationTokenForCurrentUser_hasAuthOfCorrectType_returnToken() {
        OpalJwtAuthenticationToken authentication = mock(OpalJwtAuthenticationToken.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser())
            .isEqualTo(authentication);
    }

    @Test
    void getOpalJwtAuthenticationTokenForCurrentUser_doesNotHaveAuth() {
        SecurityContextHolder.getContext().setAuthentication(null);

        UnauthorizedException exception =
            assertThrows(UnauthorizedException.class, SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);

        assertThat(exception.getTitle())
            .isEqualTo("Unauthorised");
        assertThat(exception.getDetail())
            .isEqualTo("Current user is not authenticated with OpalJwtAuthenticationToken");
    }
}
