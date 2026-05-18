package uk.gov.hmcts.opal.common.spring.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStatus;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@SpringJUnitConfig(classes = OpalMethodSecurityExpressionTest.TestConfig.class)
@AutoConfigureMockMvc
@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
class OpalMethodSecurityExpressionTest {

    private static final short TARGET_BUSINESS_UNIT_ID = 101;
    private static final short MISSING_BUSINESS_UNIT_ID = 303;
    private static final String TEST_PERMISSION = "TEST_PERM";
    private static final String OTHER_PERMISSION = "OTHER_PERM";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void hasPermission_allowsWhenPermissionExists() throws Exception {
        mockMvc.perform(get("/test/permission")
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID, TEST_PERMISSION)
                            ))))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    @Test
    void hasPermission_deniesWhenPermissionMissing() throws Exception {
        mockMvc.perform(get("/test/permission")
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID, OTHER_PERMISSION)
                            ))))
            .andExpect(status().isForbidden());
    }

    @Test
    void hasBusinessUnit_allowsWhenBusinessUnitExistsWithoutPermission() throws Exception {
        mockMvc.perform(get("/test/business-units/{businessUnitId}", TARGET_BUSINESS_UNIT_ID)
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID)
                            ))))
            .andExpect(status().isOk())
            .andExpect(content().string("ok " + TARGET_BUSINESS_UNIT_ID));
    }

    @Test
    void hasBusinessUnit_deniesWhenBusinessUnitMissing() throws Exception {
        mockMvc.perform(get("/test/business-units/{businessUnitId}", MISSING_BUSINESS_UNIT_ID)
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID)
                            ))))
            .andExpect(status().isForbidden());
    }

    @Test
    void hasPermissionInBusinessUnit_allowsWhenPermissionExistsInRequestedBusinessUnit() throws Exception {
        mockMvc.perform(get("/test/business-units/{businessUnitId}/permission", TARGET_BUSINESS_UNIT_ID)
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID, TEST_PERMISSION)
                            ))))
            .andExpect(status().isOk())
            .andExpect(content().string("ok " + TARGET_BUSINESS_UNIT_ID));
    }

    @Test
    void hasPermissionInBusinessUnit_deniesWhenPermissionMissingFromRequestedBusinessUnit() throws Exception {
        mockMvc.perform(get("/test/business-units/{businessUnitId}/permission", TARGET_BUSINESS_UNIT_ID)
                            .with(authentication(opalAuthentication(
                                businessUnit(TARGET_BUSINESS_UNIT_ID, OTHER_PERMISSION)
                            ))))
            .andExpect(status().isForbidden());
    }

    private OpalJwtAuthenticationToken opalAuthentication(BusinessUnitUser... businessUnitUsers) {
        return new OpalJwtAuthenticationToken(
            userStateWithBusinessUnits(businessUnitUsers),
            Domain.FINES,
            jwt(),
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            "details"
        );
    }

    private UserStateV2 userStateWithBusinessUnits(BusinessUnitUser... businessUnitUsers) {
        DomainBusinessUnitUsers finesUsers = DomainBusinessUnitUsers.builder()
            .businessUnitUsers(List.of(businessUnitUsers))
            .build();

        return UserStateV2.builder()
            .userId(1L)
            .username("test.user")
            .name("Test User")
            .status(UserStatus.ACTIVE)
            .version(1L)
            .cacheName("test-user-state")
            .domains(Map.of(Domain.FINES, finesUsers))
            .build();
    }

    private BusinessUnitUser businessUnit(short businessUnitId, String... permissionNames) {
        return BusinessUnitUser.builder()
            .businessUnitUserId("business-unit-user-" + businessUnitId)
            .businessUnitId(businessUnitId)
            .permissions(permissions(permissionNames))
            .build();
    }

    private Set<Permission> permissions(String... permissionNames) {
        return Arrays.stream(permissionNames)
            .map(permissionName -> Permission.builder()
                .permissionId((long)permissionName.hashCode())
                .permissionName(permissionName)
                .build())
            .collect(Collectors.toSet());
    }

    private Jwt jwt() {
        return new Jwt(
            "token-value",
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T01:00:00Z"),
            Map.of("alg", "none"),
            Map.of(JwtClaimNames.SUB, "test-subject")
        );
    }

    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import({TestController.class})
    static class TestConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

            return http
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/test/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
                .build();
        }

        @Bean
        static TestService testService() {
            return new TestService();
        }

        @Bean
        static OpalMethodSecurityExpressionHandler opalMethodSecurityExpressionHandler() {
            return new OpalMethodSecurityExpressionHandler();
        }
    }

    @RestController
    static class TestController {

        private final TestService testService;

        TestController(TestService testService) {
            this.testService = testService;
        }

        @GetMapping("/test/permission")
        public String getPermissionProtected() {
            return testService.requiresPermission();
        }

        @GetMapping("/test/business-units/{businessUnitId}")
        public String getBusinessUnitProtected(@PathVariable String businessUnitId) {
            return testService.requiresBusinessUnit(businessUnitId);
        }

        @GetMapping("/test/business-units/{businessUnitId}/permission")
        public String getBusinessUnitPermissionProtected(@PathVariable String businessUnitId) {
            return testService.requiresPermissionInBusinessUnit(businessUnitId);
        }
    }

    @Service
    static class TestService {
        @PreAuthorize("hasPermission('TEST_PERM')")
        public String requiresPermission() {
            return "ok";
        }

        @PreAuthorize("hasBusinessUnit(#businessUnitId)")
        public String requiresBusinessUnit(String businessUnitId) {
            return "ok " + businessUnitId;
        }

        @PreAuthorize("hasPermissionInBusinessUnit('TEST_PERM', #businessUnitId)")
        public String requiresPermissionInBusinessUnit(String businessUnitId) {
            return "ok " + businessUnitId;
        }
    }
}
