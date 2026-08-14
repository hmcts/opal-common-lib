package uk.gov.hmcts.opal.common.user.authorisation.client.mapper;

import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
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
    @Mapping(target = "businessUnitUser", expression = "java(flattenBusinessUnitUsersDep(userStateV2, domain))")
    @Deprecated
    UserState toUserState(UserStateV2 userStateV2, Domain domain);

    /* ACR: 2026-08-06 Note: Not convinced about this one! */
    @Mapping(source = "userStateV2.username", target = "username")
    @Mapping(source = "userStateV2.name", target = "name")
    @Mapping(target = "userStateV2.domains", expression = "java(flattenBusinessUnitUsersV2Dep(userStateV2, domain))")
    UserStateV2 toUserStateSpecific(UserStateV2 userStateV2, Domain domain);

    UserStateV2 toUserStateV2(UserStateV2Dto userStateV2Dto);

    BusinessUnitUser toBusinessUnitUser(BusinessUnitUserDto businessUnitUserDto);

    DomainBusinessUnitUsers toDomainBusinessUnitUsers(DomainDto domainDto);

    Permission toPermission(PermissionDto permissionDto);

    default PermissionV2 map(PermissionV2Dto permissionV2Dto) {
        PermissionV2 result = null;

        if (Objects.nonNull(permissionV2Dto)) {
            result = PermissionV2.fromPermissionCode(permissionV2Dto.getPermissionCode());
        }

        return result;
    }

    default Map<Domain, DomainBusinessUnitUsers> flattenBusinessUnitUsersV2Dep(UserStateV2 userStateV2, Domain domain) {
        if (Objects.isNull(userStateV2) || Objects.isNull(domain)) {
            return Map.of();
        }

        DomainBusinessUnitUsers domainBusinessUnitUsers = userStateV2.getDomains().get(domain);
        if (Objects.isNull(domainBusinessUnitUsers)) {
            return Map.of();
        }

        /*
        Collection<BusinessUnitUserV2> businessUnitUsers = domainBusinessUnitUsers.getBusinessUnitUsers();
        if (businessUnitUsers == null) {
            return Map.of();
        }
         */
        return new HashMap<>() {{
                put(domain, domainBusinessUnitUsers);
            }};
    }

    //  Temporary fix.
    default Set<BusinessUnitUser> flattenBusinessUnitUsersDep(UserStateV2 userStateV2, Domain domain) {
        if (Objects.isNull(userStateV2) || Objects.isNull(domain)) {
            return Set.of();
        }

        DomainBusinessUnitUsers domainBusinessUnitUsers = userStateV2.getDomains().get(domain);
        if (Objects.isNull(domainBusinessUnitUsers)) {
            return Set.of();
        }

        Collection<BusinessUnitUserV2> businessUnitUsers = domainBusinessUnitUsers.getBusinessUnitUsers();
        if (businessUnitUsers == null) {
            return Set.of();
        }

        return businessUnitUsers.stream()
            .filter(Objects::nonNull)
            .map(buv2 ->
                BusinessUnitUser.builder()
                    .businessUnitUserId(buv2.getBusinessUnitUserId())
                    .businessUnitId(buv2.getBusinessUnitId())
                    .permissions(castToBusinessUnitUser(buv2.getPermissions()))
                    .build()
            ).collect(Collectors.toSet());
    }

    default Set<Permission> castToBusinessUnitUser(@NonNull Set<PermissionV2> permissions) {
        return permissions.stream()
            .filter(Objects::nonNull)
            .map(UserStateMapper::castPermission)
            .collect(Collectors.toSet());
    }

    static Permission castPermission(PermissionV2 pv2) {
        if (pv2 == null) {
            return null;
        }

        return Permission
            .builder()
                .permissionId((long)pv2.ordinal())
                .permissionName(pv2.getPermissionName())
                .build();
    }

    default Set<BusinessUnitUserV2> flattenBusinessUnitUsers(UserStateV2 userStateV2, Domain domain) {
        if (domain == null || userStateV2.getDomains() == null) {
            return Set.of();
        }

        DomainBusinessUnitUsers domainBusinessUnitUsers = userStateV2.getDomains().get(domain);
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

}
