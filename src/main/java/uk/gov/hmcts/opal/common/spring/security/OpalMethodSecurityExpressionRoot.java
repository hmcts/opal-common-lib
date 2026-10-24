package uk.gov.hmcts.opal.common.spring.security;

import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

public class OpalMethodSecurityExpressionRoot extends SecurityExpressionRoot<MethodInvocation>
    implements MethodSecurityExpressionOperations {

    @Getter
    @Setter
    private Object filterObject;

    @Getter
    @Setter
    private Object returnObject;

    private Object target;

    public OpalMethodSecurityExpressionRoot(
        Supplier<? extends Authentication> authentication,
        MethodInvocation mi) {

        super(authentication, mi);
    }

    /**
     * Checks whether the current user has the named permission.
     *
     * @deprecated Use hasPermissionCode(String permissionCode) instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    @SuppressWarnings("removal")
    public boolean hasPermission(String permission) {
        return getAuthToken().hasPermission(permission);
    }

    public boolean hasPermissionCode(String permission) {
        return getAuthToken().hasPermissionCode(permission);
    }

    public boolean hasBusinessUnit(String businessUnitId) {
        return getAuthToken().hasBusinessUnit(Short.parseShort(businessUnitId));
    }

    /**
     * Checks whether the current user has the named permission in a business unit.
     *
     * @deprecated Use hasPermissionCodeInBusinessUnit(String permission, Short businessUnitId) instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    @SuppressWarnings("removal")
    public boolean hasPermissionInBusinessUnit(String permission, Short businessUnitId) {
        return getAuthToken().hasPermissionInBusinessUnit(permission, businessUnitId);
    }

    public boolean hasPermissionCodeInBusinessUnit(String permission, Short businessUnitId) {
        return getAuthToken().hasPermissionCodeInBusinessUnit(permission, businessUnitId);
    }

    private OpalJwtAuthenticationToken getAuthToken() {
        var auth = getAuthentication();

        if (auth instanceof OpalJwtAuthenticationToken opalJwtAuthenticationToken) {
            return opalJwtAuthenticationToken;
        }

        throw new IllegalStateException("Authentication object is not of type OpalJwtAuthenticationToken");
    }

    @Override
    public @Nullable Object getThis() {
        return this.target;
    }

    public void setThis(Object target) {
        this.target = target;
    }
}
