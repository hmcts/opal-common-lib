package uk.gov.hmcts.opal.common.legacy.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.opal.common.legacy.config.LegacyGatewayProperties;

/** Registers the Legacy Feign client only when a consumer opts in. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "legacy-gateway", name = "feign-enabled", havingValue = "true")
public class LegacyGatewayClientConfiguration {

    @Bean
    LegacyGatewayClient legacyGatewayClient(ApplicationContext context, LegacyGatewayProperties properties) {
        return new FeignClientBuilder(context)
            .forType(LegacyGatewayClient.class, "legacyGateway")
            .url(properties.getUrl())
            .build();
    }
}
