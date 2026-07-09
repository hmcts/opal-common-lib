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
        DomainBusinessUnitUsers.UserBusinessUnits result =
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

    private BusinessUnitUser businessUnitUser(String businessUnitUserId, Short businessUnitId,
                                              TestPermission permission) {
        return BusinessUnitUser.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnitId(businessUnitId)
            .permissions(Set.of(permission.toCommonPermission()))
            .build();
    }

    private enum TestPermission implements PermissionDescriptor {
        ACCOUNT_ENQUIRY(1L, "Account Enquiry"),
        PROCESS_PAYMENTS(2L, "Process Payments"),
        VIEW_REPORTS(3L, "View Reports");

        private final long id;
        private final String description;

        TestPermission(long id, String description) {
            this.id = id;
            this.description = description;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public String getDescription() {
            return description;
        }

        private Permission toCommonPermission() {
            return Permission.builder()
                .permissionId(id)
                .permissionName(description)
                .build();
        }
    }
}
