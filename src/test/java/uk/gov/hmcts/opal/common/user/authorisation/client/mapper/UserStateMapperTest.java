package uk.gov.hmcts.opal.common.user.authorisation.client.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsersV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStatus;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserStateMapperTest {

    private final UserStateMapper userStateMapper = Mappers.getMapper(UserStateMapper.class);

    @Test
    void toUserStateFromUserStateV2ShouldMapTopLevelFields() {
        // Arrange
        UserStateV2 userStateV2 = createUserStateV2ModelWithMultipleDomains();

        // Act
        UserState mappedUserState = userStateMapper.toUserState(userStateV2, Domain.FINES);

        //Assert
        assertAll("top-level fields from user state v2",
            () -> assertEquals(777L, mappedUserState.getUserId()),
            () -> assertEquals("v2.user", mappedUserState.getUserName()),
            () -> assertEquals("V2 User", mappedUserState.getName())
        );
    }

    @Test
    void toUserStateFromUserStateV2ShouldOnlyIncludeBusinessUnitsForRequestedDomain() {
        // Arrange
        UserStateV2 userStateV2 = createUserStateV2ModelWithMultipleDomains();

        // Act
        UserState mappedUserState = userStateMapper.toUserState(userStateV2, Domain.FINES);

        //Assert
        assertEquals(1, mappedUserState.getBusinessUnitUser().size());
        assertEquals(
            Set.of((short) 101),
            mappedUserState.getBusinessUnitUser().stream().map(BusinessUnitUser::getBusinessUnitId).collect(toSet())
        );
    }

    @Test
    void toUserStateFromUserStateV2ShouldMapNullDomainsToEmptyBusinessUnitSet() {
        // Arrange
        UserStateV2 userStateV2 = UserStateV2.builder()
            .userId(777L)
            .username("v2.user")
            .name("V2 User")
            .domains(null)
            .build();

        // Act
        UserState mappedUserState = userStateMapper.toUserState(userStateV2, Domain.FINES);

        //Assert
        assertEquals(0, mappedUserState.getBusinessUnitUser().size());
    }

    @Test
    void toUserStateFromUserStateV2ShouldMapUnknownDomainToEmptyBusinessUnitSet() {
        // Arrange
        UserStateV2 userStateV2 = createUserStateV2ModelWithMultipleDomains();

        // Act
        UserState mappedUserState = userStateMapper.toUserState(userStateV2, Domain.MAINTENANCE);

        //Assert
        assertEquals(0, mappedUserState.getBusinessUnitUser().size());
    }

    @Test
    void toUserStateFromUserStateV2ShouldMapNullDomainToEmptyBusinessUnitSet() {
        // Arrange
        UserStateV2 userStateV2 = createUserStateV2ModelWithMultipleDomains();

        // Act
        UserState mappedUserState = userStateMapper.toUserState(userStateV2, null);

        //Assert
        assertEquals(0, mappedUserState.getBusinessUnitUser().size());
    }

    @Test
    void toUserStateV2ShouldMapTopLevelFields() {
        // Arrange
        UserStateV2Dto userStateV2Dto = createUserStateV2Dto();

        // Act
        UserStateV2 mappedUserState = userStateMapper.toUserStateV2(userStateV2Dto);

        //Assert
        assertAll("top-level user state fields",
            () -> assertEquals(777L, mappedUserState.getUserId()),
            () -> assertEquals("hmcts.user", mappedUserState.getUsername()),
            () -> assertEquals("HMCTS User", mappedUserState.getName()),
            () -> assertEquals(UserStatus.ACTIVE, mappedUserState.getStatus()),
            () -> assertEquals(5L, mappedUserState.getVersion()),
            () -> assertEquals("user-state-cache", mappedUserState.getCacheName())
        );
    }

    @Test
    void toUserStateV2ShouldMapFinesDomainBusinessUnitData() {
        // Arrange
        UserStateV2Dto userStateV2Dto = createUserStateV2Dto();

        // Act
        UserStateV2 mappedUserState = userStateMapper.toUserStateV2(userStateV2Dto);

        //Assert
        DomainBusinessUnitUsersV2 finesDomainUsers = mappedUserState.getDomainBusinessUnitUsers(Domain.FINES);
        assertEquals(1, finesDomainUsers.getBusinessUnitUsers().size());
        BusinessUnitUserV2 finesBusinessUnitUser = finesDomainUsers.getBusinessUnitUsers().get(0);
        assertEquals("bu-user-101", finesBusinessUnitUser.getBusinessUnitUserId());
        assertEquals((short) 101, finesBusinessUnitUser.getBusinessUnitId());
        assertEquals(
            Set.of(
                PermissionV2.ACCOUNT_ENQUIRY,
                PermissionV2.ACCOUNT_ENQUIRY_NOTES            //  Chosen to match returned value (needs to be equal)
            ),
            finesBusinessUnitUser.getPermissions()
        );
    }

    @Test
    void toUserStateV2ShouldMapConfiscationDomainBusinessUnitData() {
        // Arrange
        UserStateV2Dto userStateV2Dto = createUserStateV2Dto();

        // Act
        UserStateV2 mappedUserState = userStateMapper.toUserStateV2(userStateV2Dto);

        //Assert
        DomainBusinessUnitUsersV2 confiscationDomainUsers = mappedUserState.getDomainBusinessUnitUsers(
            Domain.CONFISCATION
        );
        assertEquals(1, confiscationDomainUsers.getBusinessUnitUsers().size());
        BusinessUnitUserV2 confiscationBusinessUnitUser = confiscationDomainUsers.getBusinessUnitUsers().getFirst();
        assertEquals("bu-user-303", confiscationBusinessUnitUser.getBusinessUnitUserId());
        assertEquals((short) 303, confiscationBusinessUnitUser.getBusinessUnitId());
        assertEquals(
            Set.of(PermissionV2.ACCOUNT_MAINTENANCE),
            confiscationBusinessUnitUser.getPermissions()
        );
    }

    @Test
    void toDomainBusinessUnitUsersShouldMapBusinessUnitsAndPermissions() {
        // Arrange
        DomainDto domainDto = DomainDto.builder()
            .businessUnitUsers(List.of(
                new BusinessUnitUserDto(
                    "bu-user-101",
                    (short) 101,
                    List.of(new PermissionDto(1L, "PERM_A"))
                ),
                new BusinessUnitUserDto(
                    "bu-user-202",
                    (short) 202,
                    List.of(new PermissionDto(2L, "PERM_B"))
                )
            ))
            .build();

        // Act
        DomainBusinessUnitUsers mappedDomainUsers = userStateMapper.toDomainBusinessUnitUsers(domainDto);

        //Assert
        BusinessUnitUser businessUnit101 = mappedDomainUsers.getBusinessUnitUsers().stream()
            .filter(businessUnitUser -> businessUnitUser.getBusinessUnitId() == (short) 101)
            .findFirst()
            .orElseThrow();
        BusinessUnitUser businessUnit202 = mappedDomainUsers.getBusinessUnitUsers().stream()
            .filter(businessUnitUser -> businessUnitUser.getBusinessUnitId() == (short) 202)
            .findFirst()
            .orElseThrow();

        assertAll("domain business unit mapping",
            () -> assertEquals(2, mappedDomainUsers.getBusinessUnitUsers().size()),
            () -> assertEquals(
                Set.of((short) 101, (short) 202),
                mappedDomainUsers.getBusinessUnitUsers().stream().map(BusinessUnitUser::getBusinessUnitId)
                    .collect(toSet())
            ),
            () -> assertEquals("bu-user-101", businessUnit101.getBusinessUnitUserId()),
            () -> assertEquals(Set.of(Permission.builder().permissionId(1L).permissionName("PERM_A").build()),
                businessUnit101.getPermissions()),
            () -> assertEquals("bu-user-202", businessUnit202.getBusinessUnitUserId()),
            () -> assertEquals(Set.of(Permission.builder().permissionId(2L).permissionName("PERM_B").build()),
                businessUnit202.getPermissions())
        );
    }

    @Test
    void toBusinessUnitUserShouldMapFieldsAndPermissions() {
        // Arrange
        BusinessUnitUserV2Dto businessUnitUserDto = new BusinessUnitUserV2Dto(
            "bu-user-101",
            (short) 101,
            List.of(
                new PermissionV2Dto("ACCOUNT_ENQUIRY", "Account Enquiry"),
                new PermissionV2Dto("ACCOUNT_ENQUIRY_NOTES", "Account Enquiry - Account Notes")
            )
        );

        // Act
        BusinessUnitUserV2 mappedBusinessUnitUser = userStateMapper.toBusinessUnitUser(businessUnitUserDto);

        //Assert
        assertEquals("bu-user-101", mappedBusinessUnitUser.getBusinessUnitUserId());
        assertEquals((short) 101, mappedBusinessUnitUser.getBusinessUnitId());
        assertEquals(
            Set.of(
                PermissionV2.ACCOUNT_ENQUIRY,
                PermissionV2.ACCOUNT_ENQUIRY_NOTES
            ),
            mappedBusinessUnitUser.getPermissions()
        );
    }

    @Test
    void toPermissionShouldMapPermissionFields() {
        // Arrange
        PermissionDto permissionDto = new PermissionDto(9L, "PERM_X");

        // Act
        Permission mappedPermission = userStateMapper.toPermission(permissionDto);

        //Assert
        assertEquals(9L, mappedPermission.getPermissionId());
        assertEquals("PERM_X", mappedPermission.getPermissionName());
    }

    private UserStateV2 createUserStateV2ModelWithMultipleDomains() {
        PermissionV2 permA = PermissionV2.ACCOUNT_MAINTENANCE;
        PermissionV2 permB = PermissionV2.ACCOUNT_ENQUIRY;

        BusinessUnitUserV2 businessUnit101 = BusinessUnitUserV2.builder()
            .businessUnitUserId("v2-bu-user-101")
            .businessUnitId((short) 101)
            .permissions(Set.of(permA))
            .build();
        BusinessUnitUserV2 businessUnit202 = BusinessUnitUserV2.builder()
            .businessUnitUserId("v2-bu-user-202")
            .businessUnitId((short) 202)
            .permissions(Set.of(permB))
            .build();

        DomainBusinessUnitUsersV2 finesDomainBusinessUnitUsers = DomainBusinessUnitUsersV2.builder()
            .businessUnitUsers(List.of(businessUnit101))
            .build();
        DomainBusinessUnitUsersV2 confiscationDomainBusinessUnitUsers = DomainBusinessUnitUsersV2.builder()
            .businessUnitUsers(List.of(businessUnit202))
            .build();

        return UserStateV2.builder()
            .userId(777L)
            .username("v2.user")
            .name("V2 User")
            .domains(Map.of(
                Domain.FINES, finesDomainBusinessUnitUsers,
                Domain.CONFISCATION, confiscationDomainBusinessUnitUsers
            ))
            .build();
    }

    private UserStateV2Dto createUserStateV2Dto() {
        // Arrange
        DomainV2Dto finesDomain = DomainV2Dto.builder()
            .businessUnitUsers(List.of(
                new BusinessUnitUserV2Dto(
                    "bu-user-101",
                    (short) 101,
                    List.of(
                        new PermissionV2Dto("ACCOUNT_ENQUIRY", "Account Enquiry"),
                        new PermissionV2Dto("ACCOUNT_ENQUIRY_NOTES", "Account Enquiry - Account Notes")
                    )
                )
            ))
            .build();

        DomainV2Dto confiscationDomain = DomainV2Dto.builder()
            .businessUnitUsers(List.of(
                new BusinessUnitUserV2Dto(
                    "bu-user-303",
                    (short) 303,
                    List.of(new PermissionV2Dto("ACCOUNT_MAINTENANCE", "Account Maintenance"))
                )
            ))
            .build();

        return UserStateV2Dto.builder()
            .userId(777L)
            .username("hmcts.user")
            .name("HMCTS User")
            .status("ACTIVE")
            .version(5L)
            .cacheName("user-state-cache")
            .domains(Map.of(
                Domain.FINES, finesDomain,
                Domain.CONFISCATION, confiscationDomain
            ))
            .build();
    }
}
