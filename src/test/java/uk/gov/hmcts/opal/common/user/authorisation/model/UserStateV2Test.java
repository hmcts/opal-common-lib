package uk.gov.hmcts.opal.common.user.authorisation.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserStateV2Test {

    @Test
    void getDomainBusinessUnitUsers_returnsDomainDataWhenDomainExists() {
        // Arrange
        DomainBusinessUnitUsersV2 finesUsers = DomainBusinessUnitUsersV2.builder()
            .businessUnitUsers(List.of())
            .build();
        UserStateV2 userStateV2 = createUserStateV2(Map.of(Domain.FINES, finesUsers));

        //Act
        DomainBusinessUnitUsersV2 result = userStateV2.getDomainBusinessUnitUsers(Domain.FINES);

        //Assert
        assertThat(result).isSameAs(finesUsers);
    }

    @Test
    void getDomainBusinessUnitUsers_returnsEmptyBusinessUnitUsersWhenDomainsIsNull() {
        //Arrange
        UserStateV2 userStateV2 = createUserStateV2(null);

        //Act
        DomainBusinessUnitUsersV2 result = userStateV2.getDomainBusinessUnitUsers(Domain.FINES);

        //Assert
        assertThat(result).isNotNull();
        assertThat(result.getBusinessUnitUsers()).isEmpty();
    }

    @Test
    void getDomainBusinessUnitUsers_returnsEmptyBusinessUnitUsersWhenDomainIsNull() {
        //Arrange
        UserStateV2 userStateV2 = createUserStateV2(new HashMap<>());

        //Act
        DomainBusinessUnitUsersV2 result = userStateV2.getDomainBusinessUnitUsers(null);

        //Assert
        assertThat(result).isNotNull();
        assertThat(result.getBusinessUnitUsers()).isEmpty();
    }

    @Test
    void getDomainBusinessUnitUsers_returnsEmptyBusinessUnitUsersWhenDomainIsMissing() {
        //Arrange
        UserStateV2 userStateV2 = createUserStateV2(new HashMap<>());

        //Act
        DomainBusinessUnitUsersV2 result = userStateV2.getDomainBusinessUnitUsers(Domain.FINES);

        //Assert
        assertThat(result).isNotNull();
        assertThat(result.getBusinessUnitUsers()).isEmpty();
    }

    @Test
    void getDomainBusinessUnitUsers_returnsEmptyBusinessUnitUsersWhenDomainValueIsNull() {
        //Arrange
        Map<Domain, DomainBusinessUnitUsersV2> domains = new HashMap<>();
        domains.put(Domain.FINES, null);
        UserStateV2 userStateV2 = createUserStateV2(domains);

        //Act
        DomainBusinessUnitUsersV2 result = userStateV2.getDomainBusinessUnitUsers(Domain.FINES);

        //Assert
        assertThat(result).isNotNull();
        assertThat(result.getBusinessUnitUsers()).isEmpty();
    }

    @Test
    void getDomains_returnsEmptyMapWhenDomainsIsNull() {
        //Arrange
        UserStateV2 userStateV2 = createUserStateV2(null);

        //Act
        Map<Domain, DomainBusinessUnitUsersV2> result = userStateV2.getDomains();

        //Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    private UserStateV2 createUserStateV2(Map<Domain, DomainBusinessUnitUsersV2> domains) {
        return UserStateV2.builder()
            .userId(123L)
            .username("test.user")
            .domains(domains)
            .build();
    }
}
