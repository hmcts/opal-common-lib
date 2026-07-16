package uk.gov.hmcts.opal.common.operationid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.common.controllers.advice.OpalGlobalExceptionHandler;

@SpringBootTest(classes = OperationIdResponseFilterIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class OperationIdResponseFilterIntegrationTest {

    private static final String OPERATION_ID_HEADER = "operation_id";

    @Autowired
    private MockMvc mockMvc;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({OperationIdResponseFilterConfiguration.class, OpalGlobalExceptionHandler.class})
    static class TestApplication {
    }

    @Test
    void doFilterInternal_addsOperationIdToOkResponse() throws Exception {
        String operationIdHeader = mockMvc.perform(get("/test"))
            .andExpect(status().isOk()).andReturn().getResponse()
            .getHeader(OPERATION_ID_HEADER);

        assertAll(
            () -> assertNotNull(operationIdHeader),
            () -> assertThat(operationIdHeader).hasSize(32),
            () -> assertThat(operationIdHeader).doesNotContain("-")
        );
    }

    @Test
    void doFilterInternal_addsOperationIdToNoContentResponse() throws Exception {
        String operationIdHeader = mockMvc.perform(get("/no-content"))
            .andExpect(status().isNoContent()).andReturn().getResponse()
            .getHeader(OPERATION_ID_HEADER);

        assertAll(
            () -> assertNotNull(operationIdHeader),
            () -> assertThat(operationIdHeader).hasSize(32),
            () -> assertThat(operationIdHeader).doesNotContain("-")
        );
    }

    @Test
    void doFilterInternal_OperationIdMatchesBetweenHeaderAndBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/illegal-argument-error"))
            .andExpect(status().isBadRequest()).andReturn();

        String operationIdFromHeader = result.getResponse().getHeader(OPERATION_ID_HEADER);
        assertThat(operationIdFromHeader).isNotNull();

        JsonNode body = new ObjectMapper().readTree(result.getResponse().getContentAsByteArray());
        String operationIdFromBody = body.path(OPERATION_ID_HEADER).asString(null);
        assertThat(operationIdFromBody).isNotNull();

        assertThat(operationIdFromBody).isEqualTo(operationIdFromHeader);
    }

    @Test
    void doFilterInternal_addsOperationIdToUnauthorizedResponse() throws Exception {
        String operationIdHeader = mockMvc.perform(get("/secure"))
            .andExpect(status().isUnauthorized()).andExpect(unauthenticated())
            .andReturn().getResponse().getHeader(OPERATION_ID_HEADER);

        assertAll(
            () -> assertNotNull(operationIdHeader),
            () -> assertThat(operationIdHeader).hasSize(32),
            () -> assertThat(operationIdHeader).doesNotContain("-")
        );
    }
}
