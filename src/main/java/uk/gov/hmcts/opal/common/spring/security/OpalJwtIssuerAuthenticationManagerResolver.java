package uk.gov.hmcts.opal.common.spring.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;

@AllArgsConstructor
public class OpalJwtIssuerAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    private final JwtIssuerAuthenticationManagerResolver delegate;
    private final AuthenticationLogging authenticationLoggingService;

    public OpalJwtIssuerAuthenticationManagerResolver(
        AuthenticationManagerResolver<String> issuerAuthenticationManagerResolver,
        AuthenticationLogging authenticationLogging) {
        this.delegate = new JwtIssuerAuthenticationManagerResolver(issuerAuthenticationManagerResolver);
        this.authenticationLoggingService = authenticationLogging;
    }

    @Override
    public AuthenticationManager resolve(HttpServletRequest request) {
        return authentication -> {
            Authentication result;

            AuthenticationManager manager = delegate.resolve(request);
            result = manager.authenticate(authentication);

            if (result instanceof OpalJwtAuthenticationToken token
                && token.isAuthenticated()) {
                authenticationLoggingService.logSuccess(token, request);
            }
            return result;
        };
    }
}
