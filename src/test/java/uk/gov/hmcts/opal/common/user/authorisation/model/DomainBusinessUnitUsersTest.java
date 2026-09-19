package uk.gov.hmcts.opal.common.user.authorisation.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class DomainBusinessUnitUsersTest {

    private final DomainBusinessUnitUsers domainBusinessUnitUsers = DomainBusinessUnitUsers.builder()
        .businessUnitUsers(List.of(
            businessUnitUser("buu-10", (short) 10, TestPermission.ACCOUNT_ENQUIRY),
            businessUnitUser("buu-20", (short) 20, TestPermission.PROCESS_PAYMENTS),
            businessUnitUser("buu-30", (short) 30, TestPermission.PROCESS_PAYMENTS)))
        .build();

    @Test
    void anyBusinessUnitUserHasPermission_returnsTrueWhenPermissionExists() {
        assertThat(domainBusinessUnitUsers.anyBusinessUnitUserHasPermission(TestPermission.PROCESS_PAYMENTS)).isTrue();
    }

    @Test
    void noBusinessUnitUserHasPermission_returnsTrueWhenPermissionIsMissing() {
        assertThat(domainBusinessUnitUsers.noBusinessUnitUserHasPermission(TestPermission.VIEW_REPORTS)).isTrue();
    }

    @Test
    void anyBusinessUnitUserHasAnyPermission_returnsTrueWhenOnePermissionExists() {
        assertThat(domainBusinessUnitUsers.anyBusinessUnitUserHasAnyPermission(
            TestPermission.VIEW_REPORTS, TestPermission.ACCOUNT_ENQUIRY)).isTrue();
    }

    @Test
    void hasBusinessUnitUserWithPermission_returnsTrueForMatchingBusinessUnitAndPermission() {
        assertThat(domainBusinessUnitUsers.hasBusinessUnitUserWithPermission(
            (short) 20, TestPermission.PROCESS_PAYMENTS)).isTrue();
    }

    @Test
    void hasBusinessUnitUserWithAnyPermission_returnsTrueForMatchingBusinessUnitAndOnePermission() {
        assertThat(domainBusinessUnitUsers.hasBusinessUnitUserWithAnyPermission(
            (short) 10, TestPermission.VIEW_REPORTS, TestPermission.ACCOUNT_ENQUIRY)).isTrue();
    }

    @Test
    void filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions_returnsPermittedBusinessUnits() {
        Set<Short> result = domainBusinessUnitUsers.filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions(
            List.of((short) 10, (short) 20, (short) 20, (short) 40),
            TestPermission.PROCESS_PAYMENTS);

        assertThat(result).containsExactly((short) 20);
    }

    @Test
    void filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions_acceptsOptionalBusinessUnitIds() {
        Set<Short> result = domainBusinessUnitUsers.filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions(
            Optional.of(List.of((short) 10, (short) 30)),
            TestPermission.PROCESS_PAYMENTS);

        assertThat(result).containsExactly((short) 30);
    }

    @Test
    void filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions_returnsEmptySetForEmptyOptional() {
        Set<Short> result = domainBusinessUnitUsers.filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions(
            Optional.empty(),
            TestPermission.PROCESS_PAYMENTS);

        assertThat(result).isEmpty();
    }

    @Test
    void allBusinessUnitUsersWithPermission_returnsBusinessUnitsWithPermission() {
        UserStateV2.UserBusinessUnits result =
            domainBusinessUnitUsers.allBusinessUnitUsersWithPermission(TestPermission.PROCESS_PAYMENTS);

        assertThat(result.containsBusinessUnit((short) 10)).isFalse();
        assertThat(result.containsBusinessUnit((short) 20)).isTrue();
        assertThat(result.containsBusinessUnit((short) 30)).isTrue();
    }

    @Test
    void userHasPermission_returnsFalseWhenBusinessUnitUserIsMissing() {
        assertThat(DomainBusinessUnitUsers.userHasPermission(
            Optional.empty(), TestPermission.PROCESS_PAYMENTS)).isFalse();
    }

    @Test
    void userHasAnyPermission_returnsFalseWhenBusinessUnitUserIsMissing() {
        assertThat(DomainBusinessUnitUsers.userHasAnyPermission(
            Optional.empty(), TestPermission.PROCESS_PAYMENTS)).isFalse();
    }

    private BusinessUnitUserV2 businessUnitUser(String businessUnitUserId, Short businessUnitId,
                                              TestPermission permission) {
        return BusinessUnitUserV2.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnitId(businessUnitId)
            .permissions(Set.of(permission.toCommonPermission()))
            .build();
    }

    private enum TestPermission implements PermissionDescriptorV2 {
        ACCOUNT_ENQUIRY("ACCOUNT_ENQUIRY", "Account Enquiry"),
        PROCESS_PAYMENTS("PROCESS_PAYMENTS", "Process Payments"),
        VIEW_REPORTS("VIEW_REPORTS", "View Reports");

        private final String permissionCode;
        private final String permissionName;

        TestPermission(String permissionCode, String permissionName) {
            this.permissionCode = permissionCode;
            this.permissionName = permissionName;
        }

        @Override
        public String getPermissionCode() {
            return permissionCode;
        }

        @Override
        public String getPermissionName() {
            return permissionName;
        }

        private PermissionV2 toCommonPermission() {
            return PermissionV2.fromPermissionCode(permissionCode);
        }
    }
}
