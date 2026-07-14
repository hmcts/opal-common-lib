package uk.gov.hmcts.opal.common.operationid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@SpringBootTest(classes = OperationIdResponseFilterIntegrationConfiguration.class)
public class OperationIdResponseFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void doFilterInternal_addsOperationIdToResponse() throws Exception {
        String operationIdHeader = mockMvc.perform(get("/test"))
            .andExpect(status().isOk()).andReturn().getResponse()
            .getHeader("operation_id");

        assertThat(operationIdHeader).isNotNull();
    }
}
