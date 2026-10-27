package uk.gov.hmcts.opal.common.contentdigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.opal.common.util.RequestUtil;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws IOException, ServletException {
        if (RequestUtil.isMultipart(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(new CachedBodyHttpServletRequest(request), response);
    }
}
