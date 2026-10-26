package uk.gov.hmcts.opal.common.legacy.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(classes = LegacyGatewayClientNoUrlIntegrationTest.TestApplication.class)
@TestConstructor(autowireMode = ALL)
class LegacyGatewayClientNoUrlIntegrationTest {

    private final ApplicationContext context;

    LegacyGatewayClientNoUrlIntegrationTest(ApplicationContext context) {
        this.context = context;
    }

    @Test
    void doesNotRegisterClientWithoutUrlOrOptIn() {
        assertThat(context.getBeansOfType(LegacyGatewayClient.class)).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableFeignClients(basePackages = "uk.gov.hmcts.opal.common.legacy")
    @Import(LegacyGatewayClientConfiguration.class)
    static class TestApplication {
    }
}
