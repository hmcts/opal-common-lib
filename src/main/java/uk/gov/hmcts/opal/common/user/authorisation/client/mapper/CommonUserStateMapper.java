package uk.gov.hmcts.opal.common.user.authorisation.client.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

@Mapper(componentModel = "spring")
public interface CommonUserStateMapper {

    UserStateV2 toUserStateV2(UserStateV2Dto userStateDto);

    BusinessUnitUserV2 toBusinessUnitUser(BusinessUnitUserV2Dto businessUnitUserDto);

    DomainBusinessUnitUsers toDomainBusinessUnitUsers(DomainDto domainDto);

    @Mapping(target = "cacheName", ignore = true)
    @Mapping(target = "domains", expression = "java(mapUserStateV2DomainsToDto(userStateV2.getDomains()))")
    UserStateV2Dto toUserStateV2Dto(UserStateV2 userStateV2);

    default BusinessUnitUserV2Dto toBusinessUnitUserV2Dto(BusinessUnitUserV2 businessUnitUser) {
        return new BusinessUnitUserV2Dto(
            businessUnitUser.getBusinessUnitUserId(),
            businessUnitUser.getBusinessUnitId(),
            mapPermissionsToPermissionV2(businessUnitUser.getPermissions())
        );
    }

    default List<PermissionV2Dto> mapPermissionsToPermissionV2(Set<PermissionV2> permissions) {
        List<PermissionV2Dto> result = new ArrayList<>(permissions.stream()
            .filter(Objects::nonNull)
            .map(permission -> new PermissionV2Dto(permission.getPermissionCode(), permission.getPermissionName()))
            .toList());
        result.sort(Comparator.comparing(PermissionV2Dto::getPermissionCode));
        return result;
    }

    default Map<Domain, DomainDto> mapUserStateV2DomainsToDto(Map<Domain, DomainBusinessUnitUsers> domains) {
        if (domains == null || domains.isEmpty()) {
            return new EnumMap<>(Domain.class);
        }

        EnumMap<Domain, DomainDto> mappedDomains = new EnumMap<>(Domain.class);
        domains.forEach((domain, domainBusinessUnitUsers) -> {
            if (domainBusinessUnitUsers == null || domainBusinessUnitUsers.getBusinessUnitUsers() == null) {
                mappedDomains.put(domain, DomainDto.builder().businessUnitUsers(List.of()).build());
                return;
            }

            mappedDomains.put(domain, DomainDto.builder()
                .businessUnitUsers(domainBusinessUnitUsers.getBusinessUnitUsers().stream()
                    .map(this::toBusinessUnitUserV2Dto)
                    .toList())
                .build());
        });

        return mappedDomains;
    }

    default Domain toDomainOrNull(String domainName) {
        if (domainName == null || domainName.isBlank()) {
            return null;
        }
        try {
            return Domain.findByDisplayName(domainName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    default PermissionV2 toPermission(PermissionV2Dto permissionV2Dto) {
        if (permissionV2Dto == null) {
            return null;
        }

        return PermissionV2.fromPermissionCode(permissionV2Dto.getPermissionCode());
    }

}
