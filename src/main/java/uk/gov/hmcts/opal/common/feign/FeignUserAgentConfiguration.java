package uk.gov.hmcts.opal.common.feign;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration(proxyBeanMethods = false)
public class FeignUserAgentConfiguration {

    @Bean
    public RequestInterceptor userAgentRequestInterceptor(@Value("${opal.common.domain}") String domain) {
        return template -> {
            if (!template.headers().containsKey(HttpHeaders.USER_AGENT)) {
                template.header(HttpHeaders.USER_AGENT, domain);
            }
        };
    }
}
