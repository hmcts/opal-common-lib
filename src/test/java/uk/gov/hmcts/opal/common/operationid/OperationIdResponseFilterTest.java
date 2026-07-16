package uk.gov.hmcts.opal.common.operationid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.opal.common.logging.LogUtil;

class OperationIdResponseFilterTest {

    private final OperationIdResponseFilter operationIdResponseFilter = new OperationIdResponseFilter();

    @Test
    void doFilterInternal_addsOperationIdHeaderToResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn("891c44e295e44aeabc7a4d333e1e63b3");

            operationIdResponseFilter.doFilter(request, response, filterChain);

            String operationId = response.getHeader("operation_id");

            assertThat(operationId).isEqualTo("891c44e295e44aeabc7a4d333e1e63b3");
            logUtilMock.verify(LogUtil::getOrCreateOpalOperationId);
        }
    }
}
