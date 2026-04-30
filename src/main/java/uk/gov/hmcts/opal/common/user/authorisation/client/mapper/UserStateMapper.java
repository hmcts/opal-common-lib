package uk.gov.hmcts.opal.common.user.authorisation.client.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

@Mapper(componentModel = "spring")
public interface UserStateMapper {

    @Mapping(source = "username", target = "userName")
    @Mapping(source = "businessUnitUsers", target = "businessUnitUser")
    UserState toUserState(UserStateDto userStateDto);

    UserStateV2 toUserStateV2(UserStateV2Dto userStateV2Dto);

    BusinessUnitUser toBusinessUnitUser(BusinessUnitUserDto businessUnitUserDto);

    DomainBusinessUnitUsers toDomainBusinessUnitUsers(DomainDto domainDto);

    Permission toPermission(PermissionDto permissionDto);

}
