package uk.gov.hmcts.opal.common.operationid;

import org.springframework.context.annotation.Bean;

public class OperationIdResponseFilterIntegrationConfiguration {

    @Bean
    public OperationIdResponseFilter operationIdResponseFilter() {
        return new OperationIdResponseFilter();
    }

    @Bean
    public OperationIdResponseFilterController operationIdResponseFilterController() {
        return new OperationIdResponseFilterController();
    }
}
