package uk.gov.hmcts.opal.common.spring.security;

import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.util.SimpleMethodInvocation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpalMethodSecurityExpressionHandlerTest {

    @Test
    void createEvaluationContext_usesOpalRootAndPreservesMethodArguments() throws Exception {
        var handler = new OpalMethodSecurityExpressionHandler();
        var target = new SecuredService();
        var method = SecuredService.class.getMethod("requiresBusinessUnit", String.class);
        var auth = mock(OpalJwtAuthenticationToken.class);
        var invocation = new SimpleMethodInvocation(target, method, "101");

        when(auth.hasBusinessUnit("101")).thenReturn(true);

        var context = handler.createEvaluationContext(() -> auth, invocation);
        var root = context.getRootObject().getValue();

        assertThat(root).isInstanceOf(OpalMethodSecurityExpressionRoot.class);
        assertThat(((OpalMethodSecurityExpressionRoot)root).getThis()).isSameAs(target);

        var parser = new SpelExpressionParser();

        assertThat(parser.parseExpression("hasBusinessUnit(#businessUnitId)")
                       .getValue(context, boolean.class)).isTrue();
    }

    static class SecuredService {
        public void requiresBusinessUnit(String businessUnitId) {
        }
    }
}
