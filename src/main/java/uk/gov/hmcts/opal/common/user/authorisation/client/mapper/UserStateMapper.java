package uk.gov.hmcts.opal.common.user.authorisation.client.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
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
    UserState toUserState(UserStateDto userStateDto);

    @Mapping(source = "userStateV2.username", target = "userName")
    @Mapping(source = "userStateV2.name", target = "name")
    @Mapping(target = "businessUnitUser", expression = "java(flattenBusinessUnitUsers(userStateV2, domain))")
    UserState toUserState(UserStateV2 userStateV2, Domain domain);

    UserStateV2 toUserStateV2(UserStateV2Dto userStateV2Dto);

    BusinessUnitUser toBusinessUnitUser(BusinessUnitUserDto businessUnitUserDto);

    DomainBusinessUnitUsers toDomainBusinessUnitUsers(DomainDto domainDto);

    Permission toPermission(PermissionDto permissionDto);

    default Set<BusinessUnitUser> flattenBusinessUnitUsers(UserStateV2 userStateV2, Domain domain) {
        if (domain == null || userStateV2.getDomains() == null) {
            return Set.of();
        }

        DomainBusinessUnitUsers domainBusinessUnitUsers = userStateV2.getDomains().get(domain);
        if (domainBusinessUnitUsers == null) {
            return Set.of();
        }

        Collection<BusinessUnitUser> businessUnitUsers = domainBusinessUnitUsers.getBusinessUnitUsers();
        if (businessUnitUsers == null) {
            return Set.of();
        }

        return businessUnitUsers.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

}
