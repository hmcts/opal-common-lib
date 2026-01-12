package uk.gov.hmcts.opal.common.spring.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OpalJwtAuthenticationTokenTest {

    @Test
    void hasBusinessUnit_shouldReturnTrue_WhenBusinessUnitExistsInToken() {
        OpalJwtAuthenticationToken token = createOpalJwtAuthenticationToken();
        assertThat(token.hasBusinessUnit("1")).isTrue();
        assertThat(token.hasBusinessUnit("2")).isTrue();
        assertThat(token.hasBusinessUnit("3")).isTrue();
    }

    @Test
    void hasBusinessUnit_shouldReturnFalse_WhenBusinessUnitDoesNotExistsInToken() {
        OpalJwtAuthenticationToken token = createOpalJwtAuthenticationToken();
        assertThat(token.hasBusinessUnit("4")).isFalse();
    }

    @Test
    void hasPermission_shouldReturnTrue_WhenBusinessUnitExistsInToken() {
        OpalJwtAuthenticationToken token = createOpalJwtAuthenticationToken();
        assertThat(token.hasPermission("SOME_PERM1")).isTrue();
        assertThat(token.hasPermission("SOME_PERM2")).isTrue();
        assertThat(token.hasPermission("SOME_PERM3")).isTrue();
        assertThat(token.hasPermission("BU1_PERM2")).isTrue();
        assertThat(token.hasPermission("BU2_PERM2")).isTrue();
        assertThat(token.hasPermission("BU3_PERM1")).isTrue();
        assertThat(token.hasPermission("BU3_PERM2")).isTrue();
    }

    @Test
    void hasPermission_shouldReturnFalse_WhenBusinessUnitDoesNotExistsInToken() {
        OpalJwtAuthenticationToken token = createOpalJwtAuthenticationToken();
        assertThat(token.hasPermission("SOME_PERM4")).isFalse();
    }

    @Test
    void hasPermissionInBusinessUnit_shouldReturnTrue_WhenBusinessUnitHasTheAssociatedPermissionForTheUser() {
        OpalJwtAuthenticationToken token = createOpalJwtAuthenticationToken();
        assertThat(token.hasPermissionInBusinessUnit("BU1_PERM1", "1")).isTrue();
        assertThat(token.hasPermissionInBusinessUnit("BU1_PERM2", "1")).isTrue();
        assertThat(token.hasPermissionInBusinessUnit("BU2_PERM1", "2")).isTrue();
    }

    @Test
    void hasPermissionInBusinessUnit_shouldReturnFalse_WhenBusinessUnitDoesNotHaveTheAssociatedPermissionForTheUser() {
        OpalJwtAuthenticationToken token = createOpalJwtAuthenticationToken();
        assertThat(token.hasPermissionInBusinessUnit("BU2_PERM1", "1")).isFalse();
        assertThat(token.hasPermissionInBusinessUnit("BU2_PERM3", "1")).isFalse();
    }

    @Test
    void hasPermissionInBusinessUnit_shouldReturnFalse_WhenBusinessUnitDoesNotExistForTheUser() {
        OpalJwtAuthenticationToken token = createOpalJwtAuthenticationToken();
        assertThat(token.hasPermissionInBusinessUnit("BU2_PERM3", "4")).isFalse();

    }

    private OpalJwtAuthenticationToken createOpalJwtAuthenticationToken(List<String> permissions,
                                                                        Map<Short, Set<String>> businessUnitIdsToPermissionNames) {

        Set<BusinessUnitUser> businessUnitUsers = new HashSet<>();

        BusinessUnitUser permissionBusinessUnitUser = new BusinessUnitUser(null, null,
            permissions.stream().map(s -> new Permission(null, s)).collect(Collectors.toSet()));
        businessUnitUsers.add(permissionBusinessUnitUser);

        businessUnitIdsToPermissionNames.forEach((businessUnitId, strings) -> {
            BusinessUnitUser businessUnitUser = new BusinessUnitUser(null, businessUnitId,
                strings.stream().map(s -> new Permission(null, s)).collect(Collectors.toSet()));
            businessUnitUsers.add(businessUnitUser);
        });

        UserState userState = new UserState(null, null, businessUnitUsers);
        return createOpalJwtAuthenticationToken(userState);
    }

    private OpalJwtAuthenticationToken createOpalJwtAuthenticationToken() {
        return createOpalJwtAuthenticationToken(
            List.of("SOME_PERM1", "SOME_PERM2", "SOME_PERM3"),
            Map.of(
                (short) 1, Set.of("BU1_PERM1", "BU1_PERM2"),
                (short) 2, Set.of("BU2_PERM1", "BU2_PERM2"),
                (short) 3, Set.of("BU3_PERM1", "BU3_PERM2")
            ));
    }

    private OpalJwtAuthenticationToken createOpalJwtAuthenticationToken(UserState userState) {
        return new OpalJwtAuthenticationToken(userState, mock(Jwt.class), List.of());
    }
}
