package uk.gov.hmcts.opal.common.spring.security;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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

    @Test
    void hasPermission_throwsWhenAuthenticationIsNull() {
        var root = new OpalMethodSecurityExpressionRoot(() -> null, methodInvocation);

        assertThatThrownBy(() -> root.hasPermission(PERMISSION))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Authentication object cannot be null");
    }

    @Test
    void hasBusinessUnit_throwsWhenAuthenticationIsNull() {
        var root = new OpalMethodSecurityExpressionRoot(() -> null, methodInvocation);

        assertThatThrownBy(() -> root.hasBusinessUnit(BUSINESS_UNIT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Authentication object cannot be null");
    }

    @Test
    void hasPermissionInBusinessUnit_throwsWhenAuthenticationIsNull() {
        var root = new OpalMethodSecurityExpressionRoot(() -> null, methodInvocation);

        assertThatThrownBy(() -> root.hasPermissionInBusinessUnit(PERMISSION, BUSINESS_UNIT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Authentication object cannot be null");
    }

    @Test
    void hasPermission_throwsWhenAuthenticationIsWrongType() {
        var root = new OpalMethodSecurityExpressionRoot(
            () -> new TestingAuthenticationToken("hello", "world"),
            methodInvocation
        );

        assertThatThrownBy(() -> root.hasPermission(PERMISSION))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Authentication object is not of type OpalJwtAuthenticationToken");
    }

    @Test
    void hasBusinessUnit_throwsWhenAuthenticationIsWrongType() {
        var root = new OpalMethodSecurityExpressionRoot(
            () -> new TestingAuthenticationToken("hello", "world"),
            methodInvocation
        );

        assertThatThrownBy(() -> root.hasBusinessUnit(BUSINESS_UNIT))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Authentication object is not of type OpalJwtAuthenticationToken");
    }

    @Test
    void hasPermissionInBusinessUnit_throwsWhenAuthenticationIsWrongType() {
        var root = new OpalMethodSecurityExpressionRoot(
            () -> new TestingAuthenticationToken("hello", "world"),
            methodInvocation
        );

        assertThatThrownBy(() -> root.hasPermissionInBusinessUnit(PERMISSION, BUSINESS_UNIT))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Authentication object is not of type OpalJwtAuthenticationToken");
    }
}
