package uk.gov.hmcts.opal.common.user.authorisation.client.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserStateMapperTest {

    private final UserStateMapper userStateMapper = Mappers.getMapper(UserStateMapper.class);

    @Test
    void toUserStateV2ShouldMapNestedDomainAndBusinessUnitData() {
        // Arrange
        DomainDto finesDomain = DomainDto.builder()
            .businessUnitUsers(List.of(
                new BusinessUnitUserDto(
                    "bu-user-101",
                    (short) 101,
                    List.of(
                        new PermissionDto(1L, "PERM_A"),
                        new PermissionDto(2L, "PERM_B")
                    )
                )
            ))
            .build();

        DomainDto confiscationDomain = DomainDto.builder()
            .businessUnitUsers(List.of(
                new BusinessUnitUserDto(
                    "bu-user-303",
                    (short) 303,
                    List.of(new PermissionDto(3L, "PERM_C"))
                )
            ))
            .build();

        UserStateV2Dto userStateV2Dto = UserStateV2Dto.builder()
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

        // Act
        UserStateV2 mappedUserState = userStateMapper.toUserStateV2(userStateV2Dto);

        //Assert
        assertEquals(777L, mappedUserState.getUserId());
        assertEquals("hmcts.user", mappedUserState.getUsername());
        assertEquals("HMCTS User", mappedUserState.getName());
        assertEquals("ACTIVE", mappedUserState.getStatus());
        assertEquals(5L, mappedUserState.getVersion());
        assertEquals("user-state-cache", mappedUserState.getCacheName());

        DomainBusinessUnitUsers finesDomainUsers = mappedUserState.getDomainBusinessUnitUsers(Domain.FINES);
        assertEquals(1, finesDomainUsers.getBusinessUnitUsers().size());
        BusinessUnitUser finesBusinessUnitUser = finesDomainUsers.getBusinessUnitUsers().get(0);
        assertEquals("bu-user-101", finesBusinessUnitUser.getBusinessUnitUserId());
        assertEquals((short) 101, finesBusinessUnitUser.getBusinessUnitId());
        assertEquals(
            Set.of(
                Permission.builder().permissionId(1L).permissionName("PERM_A").build(),
                Permission.builder().permissionId(2L).permissionName("PERM_B").build()
            ),
            finesBusinessUnitUser.getPermissions()
        );

        DomainBusinessUnitUsers confiscationDomainUsers = mappedUserState.getDomainBusinessUnitUsers(
            Domain.CONFISCATION
        );
        assertEquals(1, confiscationDomainUsers.getBusinessUnitUsers().size());
        BusinessUnitUser confiscationBusinessUnitUser = confiscationDomainUsers.getBusinessUnitUsers().get(0);
        assertEquals("bu-user-303", confiscationBusinessUnitUser.getBusinessUnitUserId());
        assertEquals((short) 303, confiscationBusinessUnitUser.getBusinessUnitId());
        assertEquals(
            Set.of(Permission.builder().permissionId(3L).permissionName("PERM_C").build()),
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
        assertEquals(2, mappedDomainUsers.getBusinessUnitUsers().size());
        assertEquals(
            Set.of((short) 101, (short) 202),
            mappedDomainUsers.getBusinessUnitUsers().stream().map(BusinessUnitUser::getBusinessUnitId).collect(
                java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void toBusinessUnitUserShouldMapFieldsAndPermissions() {
        // Arrange
        BusinessUnitUserDto businessUnitUserDto = new BusinessUnitUserDto(
            "bu-user-101",
            (short) 101,
            List.of(
                new PermissionDto(1L, "PERM_A"),
                new PermissionDto(2L, "PERM_B")
            )
        );

        // Act
        BusinessUnitUser mappedBusinessUnitUser = userStateMapper.toBusinessUnitUser(businessUnitUserDto);

        //Assert
        assertEquals("bu-user-101", mappedBusinessUnitUser.getBusinessUnitUserId());
        assertEquals((short) 101, mappedBusinessUnitUser.getBusinessUnitId());
        assertEquals(
            Set.of(
                Permission.builder().permissionId(1L).permissionName("PERM_A").build(),
                Permission.builder().permissionId(2L).permissionName("PERM_B").build()
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
}
