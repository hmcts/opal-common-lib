package uk.gov.hmcts.opal.common.spring.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.opal.common.operationid.OperationIdResponseFilter;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;

import java.util.Map;

@Configuration
@Profile("integration")
@Import({
    MethodSecurityConfig.class,
    OpalMethodSecurityExpressionTestController.class,
    OpalMethodSecurityExpressionTestService.class
})
public class OpalMethodSecurityIntegrationConfiguration {

    private UserStateClientService userStateClientService;

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtIssuerAuthenticationManagerResolver jwtIssuerAuthenticationManagerResolver
    ) throws Exception {
        return http
            .sessionManagement(session ->
                                   session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/test/**")
                .permitAll()
                .anyRequest()
                .authenticated())
            .oauth2ResourceServer(oauth2 ->
                                      oauth2.authenticationManagerResolver(jwtIssuerAuthenticationManagerResolver)
            )
            .build();
    }

    @Bean
    static OpalMethodSecurityExpressionTestService testService() {
        return new OpalMethodSecurityExpressionTestService();
    }

    @Bean
    NimbusJwtDecoder jwtDecoder() {
        var decoder = NimbusJwtDecoder.withJwkSetUri("setUri").jwsAlgorithm(SignatureAlgorithm.RS256).build();
        var validator = JwtValidators.createDefaultWithIssuer("issuer");

        decoder.setJwtValidator(validator);

        return decoder;
    }

    @Bean
    JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter() {
        return new JwtGrantedAuthoritiesConverter();
    }

    @Bean
    OpalJwtAuthenticationProvider opalJwtAuthenticationProvider(
        NimbusJwtDecoder jwtDecoder,
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter
    ) {
        return new OpalJwtAuthenticationProvider(
            jwtDecoder,
            userStateClientService,
            jwtGrantedAuthoritiesConverter,
            Domain.FINES
        );
    }

    @Bean
    JwtIssuerAuthenticationManagerResolver jwtIssuerAuthenticationManagerResolver(
        OpalJwtAuthenticationProvider opalJwtAuthenticationProvider
    ) {
        AuthenticationManager manager = opalJwtAuthenticationProvider::authenticate;
        Map<String, AuthenticationManager> managers = Map.of("issuer", manager);

        return new JwtIssuerAuthenticationManagerResolver(managers::get);
    }

    @Bean
    public OperationIdResponseFilter operationIdResponseFilter() {
        return new OperationIdResponseFilter();
    }
}
