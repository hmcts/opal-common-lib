package uk.gov.hmcts.opal.common.spring.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Getter
public class OpalJwtAuthenticationToken extends JwtAuthenticationToken {

    private final UserState userState;

    private final Set<String> permissionNames;
    private final Map<Short, Set<String>> businessUnitIdsToPermissionNames;

    public OpalJwtAuthenticationToken(UserState userState, Jwt jwt,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(jwt, authorities, jwt.getClaimAsString(JwtClaimNames.SUB));
        this.userState = userState;

        Function<Permission, String> toPermissionNameString = permission ->
            permission.getPermissionName()
                .toUpperCase()
                .replace(" ", "_");

        this.permissionNames = userState.getBusinessUnitUser().stream()
            .flatMap(buUser -> buUser.getPermissions().stream())
            .map(toPermissionNameString)
            .collect(Collectors.toSet());

        this.businessUnitIdsToPermissionNames = userState.getBusinessUnitUser().stream()
            .collect(Collectors.toMap(
                BusinessUnitUser::getBusinessUnitId,
                buUser -> buUser.getPermissions().stream()
                    .map(toPermissionNameString)
                    .collect(Collectors.toSet())
            ));
    }

    public boolean hasBusinessUnit(String businessUnitId) {
        return businessUnitIdsToPermissionNames
            .containsKey(Short.parseShort(businessUnitId));
    }

    public boolean hasPermission(String permission) {
        return getPermissionNames().contains(permission);
    }

    public boolean hasPermissionInBusinessUnit(String permission, String businessUnitId) {
        List<String> permissionsInBusinessUnit = businessUnitIdsToPermissionNames
            .getOrDefault(Short.parseShort(businessUnitId), Set.of())
            .stream()
            .toList();
        return permissionsInBusinessUnit.contains(permission);
    }

}
