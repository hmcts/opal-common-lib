package uk.gov.hmcts.opal.common.spring.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.opal.common.spring.security.OpalAuthenticationTestUtil.businessUnit;
import static uk.gov.hmcts.opal.common.spring.security.OpalAuthenticationTestUtil.opalAuthentication;

@SpringBootTest(classes = OpalMethodSecurityIntegrationConfiguration.class)
@ActiveProfiles("integration")
@AutoConfigureMockMvc
@Import({ OpalMethodSecurityIntegrationConfiguration.class })
class OpalMethodSecurityExpressionTest {

    private static final short TARGET_BUSINESS_UNIT_ID = 101;
    private static final short OTHER_BUSINESS_UNIT_ID = 202;
    private static final short MISSING_BUSINESS_UNIT_ID = 303;
    private static final String TEST_PERMISSION = "TEST_PERM";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void hasPermission_allowsWhenPermissionExists() throws Exception {
        mockMvc.perform(get("/test/permission")
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID, TEST_PERMISSION)
                            ))))
            .andExpect(status().isOk())
            .andExpect(header().exists("operation_id"))
            .andExpect(content().string("ok"));
    }

    @Test
    void hasPermission_deniesWhenPermissionMissing() throws Exception {
        mockMvc.perform(get("/test/permission")
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID)
                            ))))
            .andExpect(header().exists("operation_id"))
            .andExpect(status().isForbidden());
    }

    @Test
    void hasBusinessUnit_allowsWhenBusinessUnitExists() throws Exception {
        mockMvc.perform(get("/test/business-units/{businessUnitId}", TARGET_BUSINESS_UNIT_ID)
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID)
                            ))))
            .andExpect(header().exists("operation_id"))
            .andExpect(status().isOk())
            .andExpect(content().string("ok " + TARGET_BUSINESS_UNIT_ID));
    }

    @Test
    void hasBusinessUnit_deniesWhenBusinessUnitMissing() throws Exception {
        mockMvc.perform(get("/test/business-units/{businessUnitId}", MISSING_BUSINESS_UNIT_ID)
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID)
                            ))))
            .andExpect(header().exists("operation_id"))
            .andExpect(status().isForbidden());
    }

    @Test
    void hasPermissionInBusinessUnit_allowsWhenPermissionExistsInRequestedBusinessUnit() throws Exception {
        mockMvc.perform(get("/test/business-units/{businessUnitId}/permission", TARGET_BUSINESS_UNIT_ID)
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID, TEST_PERMISSION)
                            ))))
            .andExpect(header().exists("operation_id"))
            .andExpect(status().isOk())
            .andExpect(content().string("ok " + TARGET_BUSINESS_UNIT_ID));
    }

    @Test
    void hasPermissionInBusinessUnit_deniesWhenPermissionMissingFromRequestedBusinessUnit() throws Exception {
        mockMvc.perform(get("/test/business-units/{businessUnitId}/permission", TARGET_BUSINESS_UNIT_ID)
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID)
                            ))))
            .andExpect(header().exists("operation_id"))
            .andExpect(status().isForbidden());
    }

    @Test
    void hasPermissionInBusinessUnit_deniesWhenPermissionExistsInDifferentBusinessUnit() throws Exception {
        mockMvc.perform(get("/test/business-units/{businessUnitId}/permission", TARGET_BUSINESS_UNIT_ID)
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID),
                                businessUnit(OTHER_BUSINESS_UNIT_ID, TEST_PERMISSION)
                            ))))
            .andExpect(header().exists("operation_id"))
            .andExpect(status().isForbidden());
    }

    @Test
    void noAuth_noAuthenticationTokenProvided() throws Exception {
        var exc = assertThrows(
            ServletException.class,
            () -> mockMvc.perform(get("/test/permission")));

        assertThat(exc)
            .hasRootCauseInstanceOf(IllegalStateException.class)
            .hasRootCauseMessage("Authentication object is not of type OpalJwtAuthenticationToken");
    }

    @Test
    void noAuth_throwsWhenAuthenticationIsNotOpalToken() {
        var nonOpalAuthentication = new TestingAuthenticationToken("user", "password");

        ServletException exception = assertThrows(
            ServletException.class,
            () -> mockMvc.perform(get("/test/permission")
                                      .with(authentication(nonOpalAuthentication)))
        );

        assertThat(exception)
            .hasRootCauseInstanceOf(IllegalStateException.class)
            .hasRootCauseMessage("Authentication object is not of type OpalJwtAuthenticationToken");
    }
}
