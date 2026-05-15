package uk.gov.hmcts.opal.common.spring.security;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class OpalMethodSecurityExpressionRootTest {
    @Mock
    private MethodInvocation mi;

    @ParameterizedTest
    @ValueSource(booleans =  {true, false})
    void hasPermission(final boolean value) {
        var token = mock(OpalJwtAuthenticationToken.class);
        var root = spy(new OpalMethodSecurityExpressionRoot(() -> token, mi));

        doReturn(token).when(root).getAuthentication();
        doReturn(value).when(token).hasPermission("PERM_123");

        assertThat(root.hasPermission("PERM_123")).isEqualTo(value);

        verify(token).hasPermission("PERM_123");
        verify(root).getAuthentication();
    }

    @ParameterizedTest
    @ValueSource(booleans =  {true, false})
    void hasBusinessUnit(final boolean value) {
        var token = mock(OpalJwtAuthenticationToken.class);
        var root = spy(new OpalMethodSecurityExpressionRoot(() -> token, mi));

        doReturn(token).when(root).getAuthentication();
        doReturn(value).when(token).hasBusinessUnit("UNIT_123");

        assertThat(root.hasBusinessUnit("UNIT_123")).isEqualTo(value);

        verify(token).hasBusinessUnit("UNIT_123");
        verify(root).getAuthentication();
    }


    @ParameterizedTest
    @ValueSource(booleans =  {true, false})
    void hasPermissionInBusinessUnit(final boolean value) {
        var token = mock(OpalJwtAuthenticationToken.class);
        var root = spy(new OpalMethodSecurityExpressionRoot(() -> token, mi));

        doReturn(token).when(root).getAuthentication();
        doReturn(value).when(token).hasPermissionInBusinessUnit("PERM_123", "UNIT_123");

        assertThat(root.hasPermissionInBusinessUnit("PERM_123", "UNIT_123")).isEqualTo(value);

        verify(token).hasPermissionInBusinessUnit("PERM_123", "UNIT_123");
        verify(root).getAuthentication();
    }
}
