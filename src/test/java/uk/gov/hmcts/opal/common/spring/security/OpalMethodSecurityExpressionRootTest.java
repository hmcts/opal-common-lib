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
    private static final String PERMISSION_CODE = "PERM_123_CODE";
    private static final String BUSINESS_UNIT = "123";

    private final MethodInvocation methodInvocation = mock(MethodInvocation.class);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @SuppressWarnings("removal")
    void hasPermission_returnsTokenPermissionResult(boolean expectedResult) {
        var token = mock(OpalJwtAuthenticationToken.class);
        var root = new OpalMethodSecurityExpressionRoot(() -> token, methodInvocation);

        when(token.hasPermission(PERMISSION)).thenReturn(expectedResult);
        assertThat(root.hasPermission(PERMISSION)).isEqualTo(expectedResult);
        verify(token).hasPermission(PERMISSION);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasPermissionCode_returnsTokenPermissionCodeResult(boolean expectedResult) {
        var token = mock(OpalJwtAuthenticationToken.class);
        var root = new OpalMethodSecurityExpressionRoot(() -> token, methodInvocation);

        when(token.hasPermissionCode(PERMISSION_CODE)).thenReturn(expectedResult);
        assertThat(root.hasPermissionCode(PERMISSION_CODE)).isEqualTo(expectedResult);
        verify(token).hasPermissionCode(PERMISSION_CODE);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasBusinessUnit_returnsTokenBusinessUnitResult(boolean expectedResult) {
        var token = mock(OpalJwtAuthenticationToken.class);
        var root = new OpalMethodSecurityExpressionRoot(() -> token, methodInvocation);

        when(token.hasBusinessUnit((short) 123)).thenReturn(expectedResult);
        assertThat(root.hasBusinessUnit(BUSINESS_UNIT)).isEqualTo(expectedResult);
        verify(token).hasBusinessUnit((short) 123);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasPermissionCodeInBusinessUnit_returnsTokenBusinessUnitPermissionCodeResult(boolean expectedResult) {
        var token = mock(OpalJwtAuthenticationToken.class);
        var root = new OpalMethodSecurityExpressionRoot(() -> token, methodInvocation);

        when(token.hasPermissionCodeInBusinessUnit(PERMISSION_CODE, (short) 123)).thenReturn(expectedResult);
        assertThat(root.hasPermissionCodeInBusinessUnit(PERMISSION_CODE, (short) 123)).isEqualTo(expectedResult);
        verify(token).hasPermissionCodeInBusinessUnit(PERMISSION_CODE, (short) 123);
    }

    @ParameterizedTest
    @SuppressWarnings("removal")
    @ValueSource(booleans = {true, false})
    void hasPermissionInBusinessUnit_returnsTokenBusinessUnitPermissionResult(boolean expectedResult) {
        var token = mock(OpalJwtAuthenticationToken.class);
        var root = new OpalMethodSecurityExpressionRoot(() -> token, methodInvocation);

        when(token.hasPermissionInBusinessUnit(PERMISSION, (short) 123)).thenReturn(expectedResult);
        assertThat(root.hasPermissionInBusinessUnit(PERMISSION, (short) 123)).isEqualTo(expectedResult);
        verify(token).hasPermissionInBusinessUnit(PERMISSION, (short) 123);
    }

    @Test
    @SuppressWarnings("removal")
    void hasPermission_throwsWhenAuthenticationIsNull() {
        var root = new OpalMethodSecurityExpressionRoot(() -> null, methodInvocation);

        assertThatThrownBy(() -> root.hasPermission(PERMISSION))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Authentication object cannot be null");
    }

    @Test
    void hasPermissionCode_throwsWhenAuthenticationIsNull() {
        var root = new OpalMethodSecurityExpressionRoot(() -> null, methodInvocation);

        assertThatThrownBy(() -> root.hasPermissionCode(PERMISSION_CODE))
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
    void hasPermissionCodeInBusinessUnit_throwsWhenAuthenticationIsNull() {
        var root = new OpalMethodSecurityExpressionRoot(() -> null, methodInvocation);

        assertThatThrownBy(() -> root.hasPermissionCodeInBusinessUnit(PERMISSION_CODE, (short) 123))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Authentication object cannot be null");
    }

    @Test
    @SuppressWarnings("removal")
    void hasPermissionInBusinessUnit_throwsWhenAuthenticationIsNull() {
        var root = new OpalMethodSecurityExpressionRoot(() -> null, methodInvocation);

        assertThatThrownBy(() -> root.hasPermissionInBusinessUnit(PERMISSION, (short) 123))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Authentication object cannot be null");
    }

    @Test
    @SuppressWarnings("removal")
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
    void hasPermissionCode_throwsWhenAuthenticationIsWrongType() {
        var root = new OpalMethodSecurityExpressionRoot(
            () -> new TestingAuthenticationToken("hello", "world"),
            methodInvocation
        );

        assertThatThrownBy(() -> root.hasPermissionCode(PERMISSION_CODE))
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
    void hasPermissionCodeInBusinessUnit_throwsWhenAuthenticationIsWrongType() {
        var root = new OpalMethodSecurityExpressionRoot(
            () -> new TestingAuthenticationToken("hello", "world"),
            methodInvocation
        );

        assertThatThrownBy(() -> root.hasPermissionCodeInBusinessUnit(PERMISSION_CODE, (short) 123))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Authentication object is not of type OpalJwtAuthenticationToken");
    }

    @Test
    @SuppressWarnings("removal")
    void hasPermissionInBusinessUnit_throwsWhenAuthenticationIsWrongType() {
        var root = new OpalMethodSecurityExpressionRoot(
            () -> new TestingAuthenticationToken("hello", "world"),
            methodInvocation
        );

        assertThatThrownBy(() -> root.hasPermissionInBusinessUnit(PERMISSION, (short) 123))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Authentication object is not of type OpalJwtAuthenticationToken");
    }
}
