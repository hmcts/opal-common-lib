package uk.gov.hmcts.opal.common.feign;

import static org.assertj.core.api.Assertions.assertThat;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

class FeignUserAgentConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(ConsumerConfiguration.class)
        .withPropertyValues("opal.common.domain=opal-fines-service");

    @ParameterizedTest
    @ValueSource(strings = {"opal-fines-service", "opal-file-handler-service"})
    void addsDomainAsDefaultUserAgent(String domain) {
        contextRunner.withPropertyValues("opal.common.domain=" + domain).run(context -> {
            assertThat(context).hasSingleBean(RequestInterceptor.class);
            RequestTemplate template = new RequestTemplate();

            context.getBean(RequestInterceptor.class).apply(template);
            context.getBean(RequestInterceptor.class).apply(template);

            assertThat(template.headers().get(HttpHeaders.USER_AGENT)).containsExactly(domain);
        });
    }

    @Test
    void preservesExplicitUserAgent() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RequestInterceptor.class);
            RequestTemplate template = new RequestTemplate();
            template.header("user-agent", "custom-client");

            context.getBean(RequestInterceptor.class).apply(template);

            assertThat(template.headers().get(HttpHeaders.USER_AGENT)).containsExactly("custom-client");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan("uk.gov.hmcts.opal.common.feign")
    static class ConsumerConfiguration {
    }
}
