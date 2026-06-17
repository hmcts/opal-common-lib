package uk.gov.hmcts.opal.common.user.authorisation.model;

import static uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FINES;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class UserStateV2TestData {

    public static final Permission VIEW_CASE = Permission.builder()
        .permissionId(1L)
        .permissionName("VIEW_CASE")
        .build();
    public static final Permission EDIT_CASE = Permission.builder()
        .permissionId(2L)
        .permissionName("EDIT_CASE")
        .build();

    private UserStateV2TestData() {
    }

    public static UserStateV2 createUserStateV2(Map<Domain, DomainBusinessUnitUsers> domains) {
        return UserStateV2.builder()
            .userId(123L)
            .username("test.user")
            .domains(domains)
            .build();
    }

    public static DomainBusinessUnitUsers createDomainBusinessUnitUsers(BusinessUnitUser... businessUnitUsers) {
        return DomainBusinessUnitUsers.builder()
            .businessUnitUsers(List.of(businessUnitUsers))
            .build();
    }

    public static BusinessUnitUser createBusinessUnitUser(
        String businessUnitUserId,
        short businessUnitId,
        Permission... permissions
    ) {
        return BusinessUnitUser.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnitId(businessUnitId)
            .permissions(Set.of(permissions))
            .build();
    }

    public static Stream<Arguments> domainLookupFallbackStates() {
        Map<Domain, DomainBusinessUnitUsers> domainsWithNullValue = new HashMap<>();
        domainsWithNullValue.put(FINES, null);

        return Stream.of(
            Arguments.of(createUserStateV2(null), FINES),
            Arguments.of(createUserStateV2(new HashMap<>()), null),
            Arguments.of(createUserStateV2(new HashMap<>()), FINES),
            Arguments.of(createUserStateV2(domainsWithNullValue), FINES)
        );
    }

    public static Stream<List<Long>> emptyBusinessUnitIdFilters() {
        return Stream.of(null, List.of());
    }
}
