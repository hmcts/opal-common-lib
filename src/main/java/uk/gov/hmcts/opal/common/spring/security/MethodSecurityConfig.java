package uk.gov.hmcts.opal.common.spring.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    @Bean
    @Primary
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new OpalMethodSecurityExpressionHandler();
    }
}
