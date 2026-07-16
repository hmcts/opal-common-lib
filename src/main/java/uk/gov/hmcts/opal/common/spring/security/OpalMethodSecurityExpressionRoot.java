package uk.gov.hmcts.opal.common.spring.security;

import lombok.Getter;
import lombok.Setter;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

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

    public boolean hasPermission(String permission) {
        System.out.println("Permission: " + permission);
        return getAuthToken().hasPermission(permission);
    }

    public boolean hasBusinessUnit(String businessUnitId) {
        return getAuthToken().hasBusinessUnit(Short.parseShort(businessUnitId));
    }

    public boolean hasPermissionInBusinessUnit(String permission, Short businessUnitId) {
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
    public @Nullable Object getThis() {
        return this.target;
    }

    public void setThis(Object target) {
        this.target = target;
    }
}
