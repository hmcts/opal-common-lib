package uk.gov.hmcts.opal.common.operationid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.opal.common.logging.LogUtil;

class OperationIdHeaderAdviceTest {

    private final OperationIdHeaderAdvice operationIdHeaderAdvice = new OperationIdHeaderAdvice();

    @Test
    void supports_returnsTrue() {
        boolean supported = operationIdHeaderAdvice.supports(null, null);
        assertTrue(supported);
    }

    @Test
    void beforeBodyWrite_addsOperationIdHeader() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);

        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn("891c44e295e44aeabc7a4d333e1e63b3");

            Object body = Map.of("OK", true);

            Object result = operationIdHeaderAdvice.beforeBodyWrite(
                body,
                null,
                MediaType.APPLICATION_JSON,
                null,
                request,
                response
            );

            assertAll(
                () -> assertThat(result).isSameAs(body),
                () -> {
                    String operationId = servletResponse.getHeader("operation_id");
                    assertThat(operationId).isEqualTo("891c44e295e44aeabc7a4d333e1e63b3");
                }
            );
            logUtilMock.verify(LogUtil::getOrCreateOpalOperationId);
        }
    }
}