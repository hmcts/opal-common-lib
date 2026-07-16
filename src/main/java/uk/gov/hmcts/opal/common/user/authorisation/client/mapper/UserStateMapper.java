package uk.gov.hmcts.opal.common.user.authorisation.client.mapper;

import java.util.HashSet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsersV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserStateMapper {

    @Mapping(source = "username", target = "userName")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "businessUnitUsers", target = "businessUnitUser")
    @Deprecated//Use toUserStateV2
    UserState toUserState(UserStateDto userStateDto);

    @Mapping(source = "userStateV2.username", target = "userName")
    @Mapping(source = "userStateV2.name", target = "name")
    @Mapping(target = "businessUnitUser", expression = "java(flattenBusinessUnitUserV2(userStateV2, domain))")
    @Deprecated
    UserState toUserState(UserStateV2 userStateV2, Domain domain);

    UserStateV2 toUserStateV2(UserStateV2Dto userStateV2Dto);

    BusinessUnitUserV2 toBusinessUnitUser(BusinessUnitUserV2Dto businessUnitUserDto);

    default PermissionV2 map(PermissionV2Dto permissionV2Dto) {
        PermissionV2 result = null;

        if (Objects.nonNull(permissionV2Dto)) {
            result = PermissionV2.fromPermissionCode(permissionV2Dto.getPermissionCode());
        }

        return result;
    }

    //BusinessUnitUserV2 toBusinessUnitUserV2(BusinessUnitUserV2Dto businessUnitUserDto);

    DomainBusinessUnitUsers toDomainBusinessUnitUsers(DomainDto domainDto);

    Permission toPermission(PermissionDto permissionDto);

    default Set<BusinessUnitUserV2> flattenBusinessUnitUsers(UserStateV2 userStateV2, Domain domain) {
        if (domain == null || userStateV2.getDomains() == null) {
            return Set.of();
        }

        DomainBusinessUnitUsersV2 domainBusinessUnitUsers = userStateV2.getDomains().get(domain);
        if (domainBusinessUnitUsers == null) {
            return Set.of();
        }

        Collection<BusinessUnitUserV2> businessUnitUsers = domainBusinessUnitUsers.getBusinessUnitUsers();
        if (businessUnitUsers == null) {
            return Set.of();
        }

        return businessUnitUsers.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    default Set<BusinessUnitUser> flattenBusinessUnitUserV2(UserStateV2 userStateV2, Domain domain) {
        if (domain == null || userStateV2.getDomains() == null) {
            return Set.of();
        }

        DomainBusinessUnitUsersV2 domainBusinessUnitUsers = userStateV2.getDomains().get(domain);
        if (domainBusinessUnitUsers == null) {
            return Set.of();
        }

        Collection<BusinessUnitUserV2> businessUnitUsers = domainBusinessUnitUsers.getBusinessUnitUsers();
        if (businessUnitUsers == null) {
            return Set.of();
        }

        HashSet<BusinessUnitUser> outcome = new HashSet<>();

        for (BusinessUnitUserV2 businessUnitUser : businessUnitUsers) {
            outcome.add(BusinessUnitUser.builder()
                    .businessUnitId(businessUnitUser.getBusinessUnitId())
                    .businessUnitUserId(businessUnitUser.getBusinessUnitUserId())
                        .build());
        }
        return outcome;
    }
}
