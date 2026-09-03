package uk.gov.hmcts.opal.common.user.authorisation.model;

import java.util.ArrayList;
import java.util.Set;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserStateV2Test {

    @Test
    void getDomainBusinessUnitUsers_returnsDomainDataWhenDomainExists() {
        // Arrange
        DomainBusinessUnitUsers finesUsers = DomainBusinessUnitUsers.builder()
            .businessUnitUsers(List.of())
            .build();
        UserStateV2 userStateV2 = createUserStateV2(Map.of(Domain.FINES, finesUsers));

        //Act
        DomainBusinessUnitUsers result = userStateV2.getDomainBusinessUnitUsers(Domain.FINES);

        //Assert
        assertThat(result).isSameAs(finesUsers);
    }

    @Test
    void getDomainBusinessUnitUsers_returnsEmptyBusinessUnitUsersWhenDomainsIsNull() {
        //Arrange
        UserStateV2 userStateV2 = createUserStateV2(null);

        //Act
        DomainBusinessUnitUsers result = userStateV2.getDomainBusinessUnitUsers(Domain.FINES);

        //Assert
        assertThat(result).isNotNull();
        assertThat(result.getBusinessUnitUsers()).isEmpty();
    }

    @Test
    void getDomainBusinessUnitUsers_returnsEmptyBusinessUnitUsersWhenDomainIsNull() {
        //Arrange
        UserStateV2 userStateV2 = createUserStateV2(new HashMap<>());

        //Act
        DomainBusinessUnitUsers result = userStateV2.getDomainBusinessUnitUsers(null);

        //Assert
        assertThat(result).isNotNull();
        assertThat(result.getBusinessUnitUsers()).isEmpty();
    }

    @Test
    void getDomainBusinessUnitUsers_returnsEmptyBusinessUnitUsersWhenDomainIsMissing() {
        //Arrange
        UserStateV2 userStateV2 = createUserStateV2(new HashMap<>());

        //Act
        DomainBusinessUnitUsers result = userStateV2.getDomainBusinessUnitUsers(Domain.FINES);

        //Assert
        assertThat(result).isNotNull();
        assertThat(result.getBusinessUnitUsers()).isEmpty();
    }

    @Test
    void getDomainBusinessUnitUsers_returnsEmptyBusinessUnitUsersWhenDomainValueIsNull() {
        //Arrange
        Map<Domain, DomainBusinessUnitUsers> domains = new HashMap<>();
        domains.put(Domain.FINES, null);
        UserStateV2 userStateV2 = createUserStateV2(domains);

        //Act
        DomainBusinessUnitUsers result = userStateV2.getDomainBusinessUnitUsers(Domain.FINES);

        //Assert
        assertThat(result).isNotNull();
        assertThat(result.getBusinessUnitUsers()).isEmpty();
    }

    @Test
    void getDomains_returnsEmptyMapWhenDomainsIsNull() {
        //Arrange
        UserStateV2 userStateV2 = createUserStateV2(null);

        //Act
        Map<Domain, DomainBusinessUnitUsers> result = userStateV2.getDomains();

        //Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testUserStateWithAnyPermission() {
        UserStateV2 userState =  createUserStateV2(new HashMap<>() {{
                put(Domain.FINES, DomainBusinessUnitUsers
                    .builder()
                    .businessUnitUsers(new ArrayList<BusinessUnitUserV2>() {{
                            add(BusinessUnitUserV2
                                .builder()
                                .businessUnitUserId("123")
                                .businessUnitId(((short) 123))
                                .permissions(Set.of())
                                .build());
                        }})
                    .build());
                }}
        );

        assertFalse(userState.hasBusinessUnitUserWithAnyPermission((short) 123, PermissionV2.CONSOLIDATE));
        assertFalse(userState.anyBusinessUnitUserHasAnyPermission(PermissionV2.CONSOLIDATE.getDescriptor()));
    }

    @Test
    void testUserStateWithBusinessUnitUserHasAnyPermission() {
        UserStateV2 userState =  createUserStateV2(new HashMap<>() {{
                put(Domain.FINES, DomainBusinessUnitUsers
                    .builder()
                    .businessUnitUsers(new ArrayList<BusinessUnitUserV2>() {{
                            add(BusinessUnitUserV2
                                .builder()
                                .businessUnitUserId("123")
                                .businessUnitId(((short) 123))
                                .permissions(Set.of(PermissionV2.CONSOLIDATE))
                                .build());
                        }})
                    .build());
            }}
        );

        assertTrue(userState.anyBusinessUnitUserHasAnyPermission(PermissionV2.CONSOLIDATE.getDescriptor()));
        assertTrue(userState.anyBusinessUnitUserHasPermission(PermissionV2.CONSOLIDATE.getDescriptor()));
        assertFalse(userState.anyBusinessUnitUserHasPermission(PermissionV2.ACCOUNT_ENQUIRY.getDescriptor()));
        assertTrue(userState.hasBusinessUnitUserWithPermission((short) 123, PermissionV2.CONSOLIDATE.getDescriptor()));
        assertTrue(userState.noBusinessUnitUserHasPermission(PermissionV2.ACCOUNT_ENQUIRY.getDescriptor()));
    }

    private UserStateV2 createUserStateV2(Map<Domain, DomainBusinessUnitUsers> domains) {
        return UserStateV2.builder()
            .userId(123L)
            .username("test.user")
            .domains(domains)
            .build();
    }
}
