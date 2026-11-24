package uk.gov.hmcts.opal.common.legacy.client;

import java.time.Duration;
import feign.Request;
import feign.Retryer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.opal.common.legacy.config.LegacyGatewayProperties;

/** Registers the opt-in Legacy Feign client with the previous gateway transport limits. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "legacy-gateway", name = "feign-enabled", havingValue = "true")
public class LegacyGatewayClientConfiguration {

    @Bean
    LegacyGatewayClient legacyGatewayClient(ApplicationContext context, LegacyGatewayProperties properties) {
        return new FeignClientBuilder(context)
            .forType(LegacyGatewayClient.class, "legacyGateway")
            .url(properties.getUrl())
            .customize(builder -> builder
                .retryer(Retryer.NEVER_RETRY)
                .options(new Request.Options(Duration.ofSeconds(30), Duration.ofSeconds(10), false)))
            .build();
    }
}
