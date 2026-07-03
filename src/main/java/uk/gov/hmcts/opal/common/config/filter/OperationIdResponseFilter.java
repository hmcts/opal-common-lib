package uk.gov.hmcts.opal.common.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.opal.common.logging.LogUtil;

@Component
@Slf4j(topic = "opal.OperationIdResponseFilter")
public class OperationIdResponseFilter extends OncePerRequestFilter {

    private static final String OPERATION_ID_HEADER = "operation_id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain)
        throws ServletException, IOException {
        String operationId = LogUtil.getOrCreateOpalOperationId();

        log.debug(":OperationIdResponseFilter: operationId={}", operationId);
        response.setHeader(OPERATION_ID_HEADER, operationId);

        filterChain.doFilter(request, response);
    }
}
