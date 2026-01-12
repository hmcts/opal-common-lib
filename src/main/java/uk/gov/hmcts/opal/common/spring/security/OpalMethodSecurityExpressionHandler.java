package uk.gov.hmcts.opal.common.spring.security;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

public class OpalMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    @Override
    public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication, MethodInvocation mi) {
        final StandardEvaluationContext ctx =
            (StandardEvaluationContext) super.createEvaluationContext(authentication, mi);

        Authentication auth = authentication.get();
        OpalMethodSecurityExpressionRoot root = new OpalMethodSecurityExpressionRoot(auth);

        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(getTrustResolver());
        root.setRoleHierarchy(getRoleHierarchy());
        root.setThis(mi.getThis());

        ctx.setRootObject(root);
        return ctx;
    }
}
