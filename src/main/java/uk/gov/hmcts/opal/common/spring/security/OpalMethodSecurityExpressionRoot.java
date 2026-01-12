package uk.gov.hmcts.opal.common.spring.security;

import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import uk.gov.hmcts.opal.common.user.authentication.SecurityUtil;

public class OpalMethodSecurityExpressionRoot extends SecurityExpressionRoot
    implements MethodSecurityExpressionOperations {


    private Object filterObject;
    private Object returnObject;
    private Object target;

    public OpalMethodSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
    }

    public boolean hasBusinessUnit(String businessUnitId) {
        OpalJwtAuthenticationToken token = getAuthenticationToken();
        return token.hasBusinessUnit(businessUnitId);
    }

    public boolean hasPermission(String permission) {
        OpalJwtAuthenticationToken token = getAuthenticationToken();
        return token.hasPermission(permission);
    }

    public boolean hasPermissionInBusinessUnit(String permission, String businessUnitId) {
        OpalJwtAuthenticationToken token = getAuthenticationToken();
        return token.hasPermissionInBusinessUnit(permission, businessUnitId);
    }

    private OpalJwtAuthenticationToken getAuthenticationToken() {
        return SecurityUtil.getAuthenticationToken(getAuthentication());
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return target;
    }

    public void setThis(Object target) {
        this.target = target;
    }
}

