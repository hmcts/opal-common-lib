package uk.gov.hmcts.opal.common.operationid;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("integration")
@EnableWebSecurity
public class OperationIdResponseFilterConfiguration {

    @Bean
    public OperationIdResponseFilter operationIdResponseFilter() {
        return new OperationIdResponseFilter();
    }

    @Bean
    public OperationIdResponseFilterController operationIdResponseFilterController() {
        return new OperationIdResponseFilterController();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.authorizeHttpRequests((auth) -> {
            auth.requestMatchers("/secure").authenticated()
                .anyRequest().permitAll();
        }).httpBasic(Customizer.withDefaults()).build();
    }
}
