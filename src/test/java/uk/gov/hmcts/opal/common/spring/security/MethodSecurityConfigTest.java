package uk.gov.hmcts.opal.common.spring.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodSecurityConfigTest {
    @Test
    void testMethodSecurityConfig() {
        assertThat(MethodSecurityConfig.methodSecurityExpressionHandler())
            .isInstanceOf(OpalMethodSecurityExpressionHandler.class);
    }
}
