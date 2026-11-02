package uk.gov.hmcts.opal.common.user.authorisation.client.mapper;

import org.mapstruct.Mapper;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

@Mapper(componentModel = "spring")
public interface UserStateMapper {

    UserStateV2 toUserStateV2(UserStateV2Dto userStateDto);

    BusinessUnitUserV2 toBusinessUnitUser(BusinessUnitUserV2Dto businessUnitUserDto);

    DomainBusinessUnitUsers toDomainBusinessUnitUsers(DomainDto domainDto);

    default PermissionV2 toPermission(PermissionV2Dto permissionV2Dto) {
        if (permissionV2Dto == null) {
            return null;
        }

        return PermissionV2.fromPermissionCode(permissionV2Dto.getPermissionCode());
    }

}
