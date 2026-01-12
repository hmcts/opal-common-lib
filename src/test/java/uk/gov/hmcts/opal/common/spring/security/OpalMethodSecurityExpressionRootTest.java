package uk.gov.hmcts.opal.common.spring.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class OpalMethodSecurityExpressionRootTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasBusinessUnit_shouldReturnValueFromTokenIndicatingIfBusinessUnitExistsOrNot(boolean value) {
        OpalJwtAuthenticationToken token = mock(OpalJwtAuthenticationToken.class);
        OpalMethodSecurityExpressionRoot expressionRoot = spy(new OpalMethodSecurityExpressionRoot(token));
        doReturn(token).when(expressionRoot).getAuthentication();
        doReturn(value).when(token).hasBusinessUnit("BU123");

        assertThat(expressionRoot.hasBusinessUnit("BU123"))
            .isEqualTo(value);

        verify(token).hasBusinessUnit("BU123");
        verify(expressionRoot).getAuthentication();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasPermission_shouldReturnValueFromTokenIndicatingIfPermissionIsLinkedToTheUserOrNot(boolean value) {
        OpalJwtAuthenticationToken token = mock(OpalJwtAuthenticationToken.class);
        OpalMethodSecurityExpressionRoot expressionRoot = spy(new OpalMethodSecurityExpressionRoot(token));
        doReturn(token).when(expressionRoot).getAuthentication();
        doReturn(value).when(token).hasPermission("PERM123");

        assertThat(expressionRoot.hasPermission("PERM123"))
            .isEqualTo(value);

        verify(token).hasPermission("PERM123");
        verify(expressionRoot).getAuthentication();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasPermissionInBusinessUnit_shouldReturnValueFromTokenIndicatingIfBusinessUnitHasTheAssociatedPermissionForTheUser(boolean value) {
        OpalJwtAuthenticationToken token = mock(OpalJwtAuthenticationToken.class);
        OpalMethodSecurityExpressionRoot expressionRoot = spy(new OpalMethodSecurityExpressionRoot(token));
        doReturn(token).when(expressionRoot).getAuthentication();
        doReturn(value).when(token).hasPermissionInBusinessUnit("PERM123","BU123");

        assertThat(expressionRoot.hasPermissionInBusinessUnit("PERM123","BU123"))
            .isEqualTo(value);

        verify(token).hasPermissionInBusinessUnit("PERM123","BU123");
        verify(expressionRoot).getAuthentication();
    }

}
