package uk.gov.hmcts.opal.common.user.authorisation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authentication.SecurityUtil;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SecurityUtil Test")
class SecurityUtilTest {

    @Test
    void getAuthenticationTokenWithParam_throwsExceptionWhenAuthenticationIsOfWrongType() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> SecurityUtil.getAuthenticationToken(mock(JwtAuthenticationToken.class)));
        assertThat(exception.getMessage())
            .isEqualTo("Authentication token is not of type OpalJwtAuthenticationToken");
    }

    @Test
    void getAuthenticationTokenWithParam_throwsExceptionWhenAuthenticationIsNull() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> SecurityUtil.getAuthenticationToken(null));
        assertThat(exception.getMessage())
            .isEqualTo("Authentication token must be provided");
    }

    @Test
    void getAuthenticationTokenWithParam_returnsTokenWhenAuthenticationIsOfCorrectType() {
        OpalJwtAuthenticationToken expectedToken = mock(OpalJwtAuthenticationToken.class);
        OpalJwtAuthenticationToken actualToken = SecurityUtil.getAuthenticationToken(expectedToken);
        assertThat(actualToken).isEqualTo(expectedToken);
    }


    @Test
    void getAuthenticationTokenWithoutParam_throwsExceptionWhenAuthenticationIsOfWrongType() {
        setSecurityContextAuthentication(mock(JwtAuthenticationToken.class));
        IllegalStateException exception =
            assertThrows(IllegalStateException.class, SecurityUtil::getAuthenticationToken);
        assertThat(exception.getMessage())
            .isEqualTo("Authentication token is not of type OpalJwtAuthenticationToken");
    }

    @Test
    void getAuthenticationTokenWithoutParam_throwsExceptionWhenAuthenticationIsNull() {
        setSecurityContextAuthentication(null);
        IllegalStateException exception =
            assertThrows(IllegalStateException.class, SecurityUtil::getAuthenticationToken);
        assertThat(exception.getMessage())
            .isEqualTo("Authentication token must be provided");
    }

    @Test
    void getAuthenticationTokenWithoutParam_returnsTokenWhenAuthenticationIsOfCorrectType() {
        OpalJwtAuthenticationToken expectedToken = mock(OpalJwtAuthenticationToken.class);
        setSecurityContextAuthentication(expectedToken);
        OpalJwtAuthenticationToken actualToken = SecurityUtil.getAuthenticationToken();
        assertThat(actualToken).isEqualTo(expectedToken);
    }

    @Test
    void getUserState_returnsUserStateWhenAuthenticationIsOfCorrectType() {
        OpalJwtAuthenticationToken expectedToken = mock(OpalJwtAuthenticationToken.class);
        UserState expectedUserState = mock(UserState.class);
        when(expectedToken.getUserState()).thenReturn(expectedUserState);

        setSecurityContextAuthentication(expectedToken);
        UserState actualUserState = SecurityUtil.getUserState();
        assertThat(actualUserState).isEqualTo(expectedUserState);

        verify(expectedToken).getUserState();
    }

    @Test
    void getUserState_throwsExceptionWhenAuthenticationIsOfWrongType() {
        setSecurityContextAuthentication(mock(JwtAuthenticationToken.class));
        IllegalStateException exception =
            assertThrows(IllegalStateException.class, SecurityUtil::getUserState);
        assertThat(exception.getMessage())
            .isEqualTo("Authentication token is not of type OpalJwtAuthenticationToken");
    }

    @Test
    void getUserState_throwsExceptionWhenAuthenticationIsNull() {
        setSecurityContextAuthentication(null);
        IllegalStateException exception =
            assertThrows(IllegalStateException.class, SecurityUtil::getUserState);
        assertThat(exception.getMessage())
            .isEqualTo("Authentication token must be provided");

    }

    private void setSecurityContextAuthentication(Authentication token) {
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
