package uk.gov.hmcts.opal.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
@Slf4j(topic = "opal.RequestInstantFilter")
public class RequestInstantFilter extends OncePerRequestFilter {

    public static final String REQUEST_RECEIVED_INSTANT = "requestReceivedInstant";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // Capture timestamp as early as possible
        Instant now = Instant.now();
        request.setAttribute(REQUEST_RECEIVED_INSTANT, now);

        log.debug("Incoming request to '{}' recorded at: {}", request.getRequestURI(), now);

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
