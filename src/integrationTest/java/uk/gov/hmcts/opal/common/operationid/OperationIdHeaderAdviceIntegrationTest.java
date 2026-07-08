package uk.gov.hmcts.opal.common.operationid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.opal.common.logging.LogUtil;

@AutoConfigureMockMvc
@SpringBootTest(classes = OperationIdHeaderAdviceIntegrationConfiguration.class)
public class OperationIdHeaderAdviceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void beforeBodyWrite_addsOperationIdToResponse() throws Exception {
        String operationId = "891c44e295e44aeabc7a4d333e1e63b3";

        try (MockedStatic<LogUtil> logUtilMock = Mockito.mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn(operationId);

            String operationIdHeader = mockMvc.perform(get("/test"))
                .andExpect(status().isOk()).andReturn().getResponse()
                .getHeader("operation_id");

            assertThat(operationIdHeader).isEqualTo("891c44e295e44aeabc7a4d333e1e63b3");
            logUtilMock.verify(LogUtil::getOrCreateOpalOperationId);
        }
    }
}
