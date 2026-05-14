package uk.gov.hmcts.opal.common.spring.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Primary;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableMethodSecurity
@EnableWebSecurity
@Configuration
public class MethodSecurityConfig {
    @Bean
    @Primary
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new OpalMethodSecurityExpressionHandler();
    }
}
