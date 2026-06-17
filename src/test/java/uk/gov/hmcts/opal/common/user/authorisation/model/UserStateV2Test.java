package uk.gov.hmcts.opal.common.user.authorisation.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FINES;
import static uk.gov.hmcts.opal.common.user.authorisation.model.Domain.USER;
import static uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2TestData.EDIT_CASE;
import static uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2TestData.VIEW_CASE;
import static uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2TestData.createBusinessUnitUser;
import static uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2TestData.createDomainBusinessUnitUsers;
import static uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2TestData.createUserStateV2;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class UserStateV2Test {

    @Nested
    class GetDomainBusinessUnitUsers {

        @Test
        void whenDomainExists_returnsDomainData_happyPath() {
            DomainBusinessUnitUsers finesUsers = createDomainBusinessUnitUsers();
            UserStateV2 userStateV2 = createUserStateV2(Map.of(FINES, finesUsers));

            DomainBusinessUnitUsers result = userStateV2.getDomainBusinessUnitUsers(FINES);

            assertThat(result).isSameAs(finesUsers);
        }

        @ParameterizedTest
        @MethodSource("uk.gov.hmcts.opal.common.user.authorisation.model"
            + ".UserStateV2TestData#domainLookupFallbackStates")
        void whenDomainDataMissing_returnsEmptyBusinessUnitUsers_sadPath(UserStateV2 userStateV2, Domain domain) {
            DomainBusinessUnitUsers result = userStateV2.getDomainBusinessUnitUsers(domain);

            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> {
                    assert result != null;
                    assertThat(result.getBusinessUnitUsers()).isEmpty();
                }
            );
        }
    }

    @Nested
    class GetDomains {

        @Test
        void whenDomainsIsNull_returnsEmptyMap_sadPath() {
            UserStateV2 userStateV2 = createUserStateV2(null);

            Map<Domain, DomainBusinessUnitUsers> result = userStateV2.getDomains();

            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result).isEmpty()
            );
        }
    }

    @Nested
    class IsBusinessUnitPermittedForCurrentUser {

        @Test
        void whenBusinessUnitIdIsNull_returnsFalse_sadPath() {
            UserStateV2 userStateV2 = createUserStateV2(Map.of(
                FINES, createDomainBusinessUnitUsers(createBusinessUnitUser("bu-1", (short) 10, VIEW_CASE))
            ));

            boolean result = userStateV2.isBusinessUnitPermittedForCurrentUser(null, FINES);

            assertThat(result).isFalse();
        }

        @Test
        void whenBusinessUnitExistsInDomain_returnsTrue_happyPath() {
            UserStateV2 userStateV2 = createUserStateV2(Map.of(
                FINES, createDomainBusinessUnitUsers(
                    createBusinessUnitUser("bu-1", (short) 10, VIEW_CASE),
                    createBusinessUnitUser("bu-2", (short) 20, EDIT_CASE)
                )
            ));

            boolean result = userStateV2.isBusinessUnitPermittedForCurrentUser((short) 20, FINES);

            assertThat(result).isTrue();
        }

        @Test
        void whenBusinessUnitMissingFromDomain_returnsFalse_sadPath() {
            UserStateV2 userStateV2 = createUserStateV2(Map.of(
                FINES, createDomainBusinessUnitUsers(createBusinessUnitUser("bu-1", (short) 10, VIEW_CASE)),
                USER, createDomainBusinessUnitUsers(createBusinessUnitUser("bu-2", (short) 20, EDIT_CASE))
            ));

            boolean result = userStateV2.isBusinessUnitPermittedForCurrentUser((short) 20, FINES);

            assertThat(result).isFalse();
        }
    }

    @Nested
    class CheckAnyBusinessUnitUserHasPermission {

        @Test
        void whenPermissionExistsInDomain_returnsTrue_happyPath() {
            UserStateV2 userStateV2 = createUserStateV2(Map.of(
                FINES, createDomainBusinessUnitUsers(
                    createBusinessUnitUser("bu-1", (short) 10, VIEW_CASE),
                    createBusinessUnitUser("bu-2", (short) 20, EDIT_CASE)
                )
            ));

            boolean result = userStateV2.checkAnyBusinessUnitUserHasPermission(EDIT_CASE, FINES);

            assertThat(result).isTrue();
        }

        @Test
        void whenPermissionIsNull_returnsFalse_sadPath() {
            UserStateV2 userStateV2 = createUserStateV2(Map.of(
                FINES, createDomainBusinessUnitUsers(createBusinessUnitUser("bu-1", (short) 10, VIEW_CASE))
            ));

            boolean result = userStateV2.checkAnyBusinessUnitUserHasPermission(null, FINES);

            assertThat(result).isFalse();
        }

        @Test
        void whenPermissionMissingFromDomain_returnsFalse_sadPath() {
            UserStateV2 userStateV2 = createUserStateV2(Map.of(
                FINES, createDomainBusinessUnitUsers(createBusinessUnitUser("bu-1", (short) 10, VIEW_CASE))
            ));

            boolean result = userStateV2.checkAnyBusinessUnitUserHasPermission(EDIT_CASE, FINES);

            assertThat(result).isFalse();
        }
    }

    @Nested
    class GetAllBusinessUnitUsersForCurrentUserForDomain {

        @ParameterizedTest
        @MethodSource("uk.gov.hmcts.opal.common.user.authorisation.model"
            + ".UserStateV2TestData#emptyBusinessUnitUserStates")
        void whenDomainHasNoUsers_returnsEmptyList_sadPath(UserStateV2 userStateV2) {
            List<BusinessUnitUser> result = userStateV2.getAllBusinessUnitUsersForCurrentUserForDomain(FINES);

            assertThat(result).isEmpty();
        }

        @Test
        void whenDomainHasUsers_returnsBusinessUnitUsers_happyPath() {
            BusinessUnitUser firstUser = createBusinessUnitUser("bu-1", (short) 10, VIEW_CASE);
            BusinessUnitUser secondUser = createBusinessUnitUser("bu-2", (short) 20, EDIT_CASE);
            UserStateV2 userStateV2 = createUserStateV2(Map.of(
                FINES, createDomainBusinessUnitUsers(firstUser, secondUser)
            ));

            List<BusinessUnitUser> result = userStateV2.getAllBusinessUnitUsersForCurrentUserForDomain(FINES);

            assertThat(result).containsExactly(firstUser, secondUser);
        }
    }

    @Nested
    class GetBusinessUnitUsersForBusinessUnitIds {

        @ParameterizedTest
        @MethodSource("uk.gov.hmcts.opal.common.user.authorisation.model"
            + ".UserStateV2TestData#emptyBusinessUnitIdFilters")
        void whenBusinessUnitIdsEmpty_returnsEmptyList_sadPath(List<Long> businessUnitIds) {
            UserStateV2 userStateV2 = createUserStateV2(Map.of(
                FINES, createDomainBusinessUnitUsers(createBusinessUnitUser("bu-1", (short) 10, VIEW_CASE))
            ));

            List<BusinessUnitUser> result = userStateV2.getBusinessUnitUsersForBusinessUnitIds(businessUnitIds, FINES);

            assertThat(result).isEmpty();
        }

        @Test
        void whenBusinessUnitIdsMatch_returnsMatchingBusinessUnitUsers_happyPath() {
            BusinessUnitUser firstUser = createBusinessUnitUser("bu-1", (short) 10, VIEW_CASE);
            BusinessUnitUser secondUser = createBusinessUnitUser("bu-2", (short) 20, EDIT_CASE);
            BusinessUnitUser thirdUser = createBusinessUnitUser("bu-3", (short) 30, VIEW_CASE, EDIT_CASE);
            UserStateV2 userStateV2 = createUserStateV2(Map.of(
                FINES, createDomainBusinessUnitUsers(firstUser, secondUser, thirdUser)
            ));

            List<BusinessUnitUser> result =
                userStateV2.getBusinessUnitUsersForBusinessUnitIds(List.of(10L, 30L), FINES);

            assertThat(result).containsExactly(firstUser, thirdUser);
        }
    }
}
