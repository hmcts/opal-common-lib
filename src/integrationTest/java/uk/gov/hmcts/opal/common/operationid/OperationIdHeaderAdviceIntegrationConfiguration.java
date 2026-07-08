package uk.gov.hmcts.opal.common.operationid;

import org.springframework.context.annotation.Bean;

public class OperationIdHeaderAdviceIntegrationConfiguration {

    @Bean
    public OperationIdHeaderAdvice operationIdHeaderAdvice() {
        return new OperationIdHeaderAdvice();
    }

    @Bean
    public OperationIdHeaderAdviceController operationIdHeaderAdviceController() {
        return new OperationIdHeaderAdviceController();
    }
}
