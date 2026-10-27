package uk.gov.hmcts.opal.common.docmosis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.opal.common.config.DocmosisConfig;

/** Registers the Feign client only when configured; Feign scans annotated clients before evaluating conditions. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "opal.common.docmosis", name = {"url", "api-key"})
public class DocmosisClientConfiguration {

    @Bean
    DocmosisClient docmosisClient(ApplicationContext context, DocmosisConfig config) {
        return new FeignClientBuilder(context)
            .forType(DocmosisClient.class, "docmosisClient")
            .url(config.getUrl())
            .build();
    }
}
