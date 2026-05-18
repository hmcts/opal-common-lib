package uk.gov.hmcts.opal.common.spring.security;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpalMethodSecurityExpressionRootTest {

    private static final String PERMISSION = "PERM_123";
    private static final String BUSINESS_UNIT = "UNIT_123";

    private final MethodInvocation methodInvocation = mock(MethodInvocation.class);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasPermission_returnsTokenPermissionResult(boolean expectedResult) {
        var token = mock(OpalJwtAuthenticationToken.class);
        var root = new OpalMethodSecurityExpressionRoot(() -> token, methodInvocation);

        when(token.hasPermission(PERMISSION)).thenReturn(expectedResult);
        assertThat(root.hasPermission(PERMISSION)).isEqualTo(expectedResult);
        verify(token).hasPermission(PERMISSION);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasBusinessUnit_returnsTokenBusinessUnitResult(boolean expectedResult) {
        var token = mock(OpalJwtAuthenticationToken.class);
        var root = new OpalMethodSecurityExpressionRoot(() -> token, methodInvocation);

        when(token.hasBusinessUnit(BUSINESS_UNIT)).thenReturn(expectedResult);
        assertThat(root.hasBusinessUnit(BUSINESS_UNIT)).isEqualTo(expectedResult);
        verify(token).hasBusinessUnit(BUSINESS_UNIT);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasPermissionInBusinessUnit_returnsTokenBusinessUnitPermissionResult(boolean expectedResult) {
        var token = mock(OpalJwtAuthenticationToken.class);
        var root = new OpalMethodSecurityExpressionRoot(() -> token, methodInvocation);

        when(token.hasPermissionInBusinessUnit(PERMISSION, BUSINESS_UNIT)).thenReturn(expectedResult);
        assertThat(root.hasPermissionInBusinessUnit(PERMISSION, BUSINESS_UNIT)).isEqualTo(expectedResult);
        verify(token).hasPermissionInBusinessUnit(PERMISSION, BUSINESS_UNIT);
    }
}
