package uk.gov.hmcts.opal.common.user.authorisation.client.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStatus;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommonUserStateMapperTest {

    private final CommonUserStateMapper commonUserStateMapper = Mappers.getMapper(CommonUserStateMapper.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toUserStateV2ShouldMapTopLevelFields() {
        // Arrange
        UserStateV2Dto userStateV2Dto = createUserStateV2Dto();

        // Act
        UserStateV2 mappedUserState = commonUserStateMapper.toUserStateV2(userStateV2Dto);

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
        UserStateV2 mappedUserState = commonUserStateMapper.toUserStateV2(userStateV2Dto);

        //Assert
        DomainBusinessUnitUsers finesDomainUsers = mappedUserState.getDomainBusinessUnitUsers(Domain.FINES);
        assertEquals(1, finesDomainUsers.getBusinessUnitUsers().size());
        BusinessUnitUserV2 finesBusinessUnitUser = finesDomainUsers.getBusinessUnitUsers().getFirst();
        assertEquals("bu-user-101", finesBusinessUnitUser.getBusinessUnitUserId());
        assertEquals((short) 101, finesBusinessUnitUser.getBusinessUnitId());
        assertEquals(
            Set.of(
                PermissionV2.ACCOUNT_ENQUIRY,
                PermissionV2.ACCOUNT_ENQUIRY_NOTES
            ),
            finesBusinessUnitUser.getPermissions()
        );
    }

    @Test
    void toUserStateV2ShouldMapConfiscationDomainBusinessUnitData() {
        // Arrange
        UserStateV2Dto userStateV2Dto = createUserStateV2Dto();

        // Act
        UserStateV2 mappedUserState = commonUserStateMapper.toUserStateV2(userStateV2Dto);

        //Assert
        DomainBusinessUnitUsers confiscationDomainUsers = mappedUserState.getDomainBusinessUnitUsers(
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
                new BusinessUnitUserV2Dto(
                    "bu-user-101",
                    (short) 101,
                    List.of(new PermissionV2Dto("ACCOUNT_ENQUIRY", "Account Enquiry"))
                ),
                new BusinessUnitUserV2Dto(
                    "bu-user-202",
                    (short) 202,
                    List.of(new PermissionV2Dto("ACCOUNT_ENQUIRY_NOTES", "Account Enquiry - Account Notes"))
                )
            ))
            .build();

        // Act
        DomainBusinessUnitUsers mappedDomainUsers = commonUserStateMapper.toDomainBusinessUnitUsers(domainDto);

        //Assert
        BusinessUnitUserV2 businessUnit101 = mappedDomainUsers.getBusinessUnitUsers().stream()
            .filter(businessUnitUser -> businessUnitUser.getBusinessUnitId() == (short) 101)
            .findFirst()
            .orElseThrow();
        BusinessUnitUserV2 businessUnit202 = mappedDomainUsers.getBusinessUnitUsers().stream()
            .filter(businessUnitUser -> businessUnitUser.getBusinessUnitId() == (short) 202)
            .findFirst()
            .orElseThrow();

        assertAll("domain business unit mapping",
            () -> assertEquals(2, mappedDomainUsers.getBusinessUnitUsers().size()),
            () -> assertEquals(
                Set.of((short) 101, (short) 202),
                mappedDomainUsers.getBusinessUnitUsers().stream().map(BusinessUnitUserV2::getBusinessUnitId)
                    .collect(toSet())
            ),
            () -> assertEquals("bu-user-101", businessUnit101.getBusinessUnitUserId()),
            () -> assertEquals(Set.of(PermissionV2.fromPermissionName("Account Enquiry")),
                businessUnit101.getPermissions()),
            () -> assertEquals("bu-user-202", businessUnit202.getBusinessUnitUserId()),
            () -> assertEquals(Set.of(PermissionV2.fromPermissionName("Account Enquiry - Account Notes")),
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
        BusinessUnitUserV2 mappedBusinessUnitUser = commonUserStateMapper.toBusinessUnitUser(businessUnitUserDto);

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
        PermissionV2Dto permissionDto = new PermissionV2Dto("ACCOUNT_MAINTENANCE", "Account Maintenance");

        // Act
        PermissionV2 mappedPermission = commonUserStateMapper.toPermission(permissionDto);

        //Assert
        assertEquals("ACCOUNT_MAINTENANCE", mappedPermission.getPermissionCode());
        assertEquals("Account Maintenance", mappedPermission.getPermissionName());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void toUserStateV2Dto_fromUserStateV2_mapsCacheNameAndDomains() throws JacksonException {

        // Arrange
        UserStateV2 userStateV2 = UserStateV2.builder()
            .userId(123L)
            .username("username")
            .name("token")
            .status(UserStatus.ACTIVE)
            .version(321L)
            .systemUser(true)
            .cacheName("USER_STATE_subject-123")
            .domains(java.util.Map.of(
                Domain.FINES, DomainBusinessUnitUsers.builder()
                    .businessUnitUsers(List.of(
                        BusinessUnitUserV2.builder()
                            .businessUnitUserId("ABC123")
                            .businessUnitId((short) 41)
                            .permissions(emptySet())
                            .build()
                    ))
                    .build()
            ))
            .build();

        // Act
        UserStateV2Dto dto = commonUserStateMapper.toUserStateV2Dto(userStateV2);

        // Assert
        assertNull(dto.getCacheName());
        assertEquals(objectMapper.readTree("""
                {
                  "user_id": 123,
                  "username": "username",
                  "name": "token",
                  "status": "ACTIVE",
                  "version": 321,
                  "cache_name": null,
                  "is_system_user": true,
                  "domains": {
                    "fines": {
                      "business_unit_users": [
                        {
                          "business_unit_user_id": "ABC123",
                          "business_unit_id": 41,
                          "permissions": []
                        }
                      ]
                    }
                  }
                }
            """), objectMapper.readTree(objectMapper.writeValueAsString(dto)));
    }

    private UserStateV2 createUserStateV2ModelWithMultipleDomains() {
        PermissionV2 permA = PermissionV2.ACCOUNT_ENQUIRY;
        PermissionV2 permB = PermissionV2.ACCOUNT_ENQUIRY_NOTES;

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

        DomainBusinessUnitUsers finesDomainBusinessUnitUsers = DomainBusinessUnitUsers.builder()
            .businessUnitUsers(List.of(businessUnit101))
            .build();
        DomainBusinessUnitUsers confiscationDomainBusinessUnitUsers = DomainBusinessUnitUsers.builder()
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
        DomainDto finesDomain = DomainDto.builder()
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

        DomainDto confiscationDomain = DomainDto.builder()
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
