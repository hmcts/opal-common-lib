package uk.gov.hmcts.opal.common.contentdigest;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(ContentDigestProperties.class)
public class ContentDigestConfiguration {

    @Bean
    ContentDigestValidatorInterceptor contentDigestValidatorInterceptor(ContentDigestProperties properties) {
        return new ContentDigestValidatorInterceptor(properties);
    }

    @Bean
    RequestCachingFilter requestCachingFilter() {
        return new RequestCachingFilter();
    }

    @Bean
    FilterRegistrationBean<RequestCachingFilter> requestCachingFilterRegistration(RequestCachingFilter filter) {
        FilterRegistrationBean<RequestCachingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
        return registration;
    }

    @Bean
    ContentDigestResponseFilter contentDigestResponseFilter(ContentDigestProperties properties) {
        return new ContentDigestResponseFilter(properties);
    }

    @Bean
    WebMvcConfigurer contentDigestWebMvcConfigurer(ContentDigestValidatorInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}
