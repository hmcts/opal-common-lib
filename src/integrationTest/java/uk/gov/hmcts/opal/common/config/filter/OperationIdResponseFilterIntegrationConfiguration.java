package uk.gov.hmcts.opal.common.config.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OperationIdResponseFilterIntegrationConfiguration {

    @Bean
    public OperationIdResponseFilter operationIdResponseFilter() {
        return new OperationIdResponseFilter();
    }

    @Bean
    public OperationIdIntegrationController operationIdIntegrationController() {
        return new OperationIdIntegrationController();
    }
}
