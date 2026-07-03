package uk.gov.hmcts.opal.common.config.filter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OperationIdResponseFilterTest {

    private final OperationIdResponseFilter operationIdResponseFilter = new OperationIdResponseFilter();

    @Test
    @DisplayName("Should generate operation ID for response")
    void shouldGenerateOperationIdForResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        operationIdResponseFilter.doFilter(request, response, filterChain);
        String operationId = response.getHeader("operation_id");

        assertThat(operationId).isNotNull().hasSize(32).doesNotContain("-");
    }
}
