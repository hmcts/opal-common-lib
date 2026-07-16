package uk.gov.hmcts.opal.common.spring.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsersV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptorV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
public class OpalJwtAuthenticationToken extends JwtAuthenticationToken {

    @Getter
    private final UserStateV2 userState;
    private final Set<String> permissionNames;
    private final Map<Short, Set<String>> businessUnitIdsToPermissionNames;

    public OpalJwtAuthenticationToken(UserStateV2 userState, Domain domain, Jwt jwt,
                                      Collection<? extends GrantedAuthority> authorities, Object details) {

        super(jwt, authorities, jwt.getClaimAsString(JwtClaimNames.SUB));
        setDetails(details);
        this.userState = userState;


        DomainBusinessUnitUsersV2 domainBusinessUnitUsers = userState.getDomainBusinessUnitUsers(domain);

        this.permissionNames = domainBusinessUnitUsers.getBusinessUnitUsers().stream()
            .flatMap(buUser -> buUser.getPermissions().stream())
            .map(this::toPermissionNameString)
            .collect(Collectors.toSet());

        this.businessUnitIdsToPermissionNames = domainBusinessUnitUsers.getBusinessUnitUsers().stream()
            .collect(Collectors.toMap(
                BusinessUnitUserV2::getBusinessUnitId,
                buUser -> buUser.getPermissions().stream()
                    .map(this::toPermissionNameString)
                    .collect(Collectors.toSet())
            ));
    }

    public String toPermissionNameString(PermissionDescriptor permissionDescriptor) {
        return permissionDescriptor.getDescription()
            .toUpperCase()
            .replace(" ", "_");
    }

    public String toPermissionNameString(PermissionDescriptorV2 permissionDescriptor) {
        return permissionDescriptor.getPermissionName()
            .toUpperCase()
            .replace(" ", "_");
    }

    public Long getUserId() {
        return userState.getUserId();
    }

    public String getUsername() {
        return userState.getUsername();
    }

    public String getUserStateName() {
        return userState.getName();
    }

    public UserStatus getStatus() {
        return userState.getStatus();
    }

    public Long getVersion() {
        return userState.getVersion();
    }

    public String getCacheName() {
        return userState.getCacheName();
    }

    public boolean hasBusinessUnit(short businessUnitId) {
        return businessUnitIdsToPermissionNames
            .containsKey(businessUnitId);
    }

    public boolean hasPermission(String permission) {
        return permissionNames.contains(permission);
    }

    public boolean hasPermission(PermissionDescriptorV2 permission) {
        return hasPermission(toPermissionNameString(permission));
    }

    public boolean hasAtLeastOneOfPermission(String... permissions) {
        return Arrays.stream(permissions).anyMatch(this::hasPermission);
    }

    public boolean hasAtLeastOneOfPermission(PermissionDescriptorV2... permissions) {
        return Arrays.stream(permissions).anyMatch(this::hasPermission);
    }


    public boolean hasPermissionInBusinessUnit(String permission, Short businessUnitId) {
        List<String> permissionsInBusinessUnit = businessUnitIdsToPermissionNames
            .getOrDefault(businessUnitId, Set.of())
            .stream()
            .toList();
        return permissionsInBusinessUnit.contains(permission);
    }

    public boolean hasPermissionInBusinessUnit(PermissionDescriptorV2 permission, Short businessUnitId) {
        return hasPermissionInBusinessUnit(toPermissionNameString(permission), businessUnitId);
    }

    public boolean hasAtLeastOneOfPermissionInBusinessUnit(Short businessUnitId,
        PermissionDescriptorV2... permissions) {
        return Arrays.stream(permissions).anyMatch(p -> hasPermissionInBusinessUnit(p, businessUnitId));
    }

    public boolean hasAtLeastOneOfPermissionInBusinessUnit(Short businessUnitId, String... permissions) {
        return Arrays.stream(permissions).anyMatch(p -> hasPermissionInBusinessUnit(p, businessUnitId));
    }
}
