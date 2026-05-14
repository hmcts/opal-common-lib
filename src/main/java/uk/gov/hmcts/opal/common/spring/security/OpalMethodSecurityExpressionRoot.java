package uk.gov.hmcts.opal.common.spring.security;

import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

public class OpalMethodSecurityExpressionRoot extends SecurityExpressionRoot<MethodInvocation>
    implements MethodSecurityExpressionOperations {

    private Object filterObject;
    private Object returnObject;
    private Object target;

    public OpalMethodSecurityExpressionRoot(Supplier<? extends Authentication> authentication,
                                            MethodInvocation mi) {
        super(authentication, mi);
    }

    public boolean hasPermission(String permission) {
        return getAuthToken().hasPermission(permission);
    }

    public boolean hasBusinessUnit(String businessUnitId) {
        return getAuthToken().hasBusinessUnit(businessUnitId);
    }

    public boolean hasPermissionInBusinessUnit(String permission, String businessUnitId) {
        return getAuthToken().hasPermissionInBusinessUnit(permission, businessUnitId);
    }

    private OpalJwtAuthenticationToken getAuthToken() {
        var auth = getAuthentication();

        if (auth instanceof OpalJwtAuthenticationToken opalJwtAuthenticationToken) {
            return opalJwtAuthenticationToken;
        }

        throw new IllegalStateException("Authentication object is not of type OpalJwtAuthenticationToken");
    }

    @Override
    public void setFilterObject(@Nullable Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public @Nullable Object getFilterObject() {
        return this.filterObject;
    }

    @Override
    public void setReturnObject(@Nullable Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public @Nullable Object getReturnObject() {
        return this.returnObject;
    }

    @Override
    public @Nullable Object getThis() {
        return this.target;
    }

    public void setThis(Object target) {
        this.target = target;
    }
}
