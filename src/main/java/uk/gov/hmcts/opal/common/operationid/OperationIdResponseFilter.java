package uk.gov.hmcts.opal.common.operationid;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.opal.common.logging.LogUtil;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j(topic = "opal.OperationIdResponseFilter")
public class OperationIdResponseFilter extends OncePerRequestFilter {

    private static final String OPERATION_ID_HEADER = "operation_id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String operationId = LogUtil.getOrCreateOpalOperationId();
            log.debug(":OperationIdResponseFilter: {}={}", OPERATION_ID_HEADER, operationId);
            response.setHeader(OPERATION_ID_HEADER, operationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("opal-operation-id");
        }
    }
}
