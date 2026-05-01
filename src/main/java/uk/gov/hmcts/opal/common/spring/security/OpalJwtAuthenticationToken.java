package uk.gov.hmcts.opal.common.spring.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

        Function<Permission, String> toPermissionNameString = permission ->
            permission.getPermissionName()
                .toUpperCase()
                .replace(" ", "_");

        DomainBusinessUnitUsers domainBusinessUnitUsers = userState.getDomainBusinessUnitUsers(domain);

        this.permissionNames = domainBusinessUnitUsers.getBusinessUnitUsers().stream()
            .flatMap(buUser -> buUser.getPermissions().stream())
            .map(toPermissionNameString)
            .collect(Collectors.toSet());

        this.businessUnitIdsToPermissionNames = domainBusinessUnitUsers.getBusinessUnitUsers().stream()
            .collect(Collectors.toMap(
                BusinessUnitUser::getBusinessUnitId,
                buUser -> buUser.getPermissions().stream()
                    .map(toPermissionNameString)
                    .collect(Collectors.toSet())
            ));
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

    public String getStatus() {
        return userState.getStatus();
    }

    public Long getVersion() {
        return userState.getVersion();
    }

    public String getCacheName() {
        return userState.getCacheName();
    }

    public boolean hasBusinessUnit(String businessUnitId) {
        return businessUnitIdsToPermissionNames
            .containsKey(Short.parseShort(businessUnitId));
    }

    public boolean hasPermission(String permission) {
        return permissionNames.contains(permission);
    }

    public boolean hasPermissionInBusinessUnit(String permission, String businessUnitId) {
        List<String> permissionsInBusinessUnit = businessUnitIdsToPermissionNames
            .getOrDefault(Short.parseShort(businessUnitId), Set.of())
            .stream()
            .toList();
        return permissionsInBusinessUnit.contains(permission);
    }

}
