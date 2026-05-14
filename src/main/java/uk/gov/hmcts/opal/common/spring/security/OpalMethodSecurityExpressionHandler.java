package uk.gov.hmcts.opal.common.spring.security;

import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

public class OpalMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {
    @Override
    public EvaluationContext createEvaluationContext(Supplier<? extends @Nullable Authentication> authentication,
                                                     MethodInvocation mi) {
        var context = (StandardEvaluationContext)super.createEvaluationContext(authentication, mi);
        var root = new OpalMethodSecurityExpressionRoot(authentication, mi);

        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setThis(mi.getThis());

        context.setRootObject(root);

        return context;
    }
}
