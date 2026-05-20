package uk.gov.hmcts.opal.common.spring.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
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

public final class OpalAuthenticationTestUtil {

    public static OpalJwtAuthenticationToken opalAuthentication(BusinessUnitUser... businessUnitUsers) {
        return new OpalJwtAuthenticationToken(
            userStateWithBusinessUnits(businessUnitUsers), Domain.FINES, jwt(), List.of(), "details");
    }

    public static BusinessUnitUser businessUnit(short businessUnitId, String... permissionNames) {
        return BusinessUnitUser.builder()
            .businessUnitUserId("business-unit-user-" + businessUnitId)
            .businessUnitId(businessUnitId)
            .permissions(permissions(permissionNames))
            .build();
    }

    private static UserStateV2 userStateWithBusinessUnits(BusinessUnitUser... businessUnitUsers) {
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

    private static Set<Permission> permissions(String... permissionNames) {
        return Arrays.stream(permissionNames)
            .map(permissionName -> Permission.builder()
                .permissionId((long)permissionName.hashCode())
                .permissionName(permissionName)
                .build())
            .collect(Collectors.toSet());
    }

    private static Jwt jwt() {
        return new Jwt(
            "token-value",
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T01:00:00Z"),
            Map.of("alg", "none"),
            Map.of(JwtClaimNames.SUB, "test-subject")
        );
    }
}
