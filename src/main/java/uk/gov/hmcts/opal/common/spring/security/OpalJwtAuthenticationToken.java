package uk.gov.hmcts.opal.common.spring.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStatus;

@EqualsAndHashCode(callSuper = true)
public class OpalJwtAuthenticationToken extends JwtAuthenticationToken {

    @Getter
    private final UserStateV2 userState;

    /**
     * For removal.
     *
     * @deprecated Use permissionCodes instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    private final Set<String> permissionNames;

    private final Set<String> permissionCodes;

    /**
     * For removal.
     *
     * @deprecated Use businessUnitIdsToPermissionCodes instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    private final Map<Short, Set<String>> businessUnitIdsToPermissionNames;
    private final Map<Short, Set<String>> businessUnitIdsToPermissionCodes;

    public OpalJwtAuthenticationToken(UserStateV2 userState, Domain domain, Jwt jwt,
        Collection<? extends GrantedAuthority> authorities, Object details) {

        super(jwt, authorities, jwt.getClaimAsString(JwtClaimNames.SUB));
        setDetails(details);
        this.userState = userState;

        DomainBusinessUnitUsers domainBusinessUnitUsers = userState.getDomainBusinessUnitUsers(domain);

        this.permissionNames = domainBusinessUnitUsers.getBusinessUnitUsers().stream()
            .flatMap(buUser -> buUser.getPermissions().stream())
            .map(this::toPermissionNameString)
            .collect(Collectors.toSet());

        this.businessUnitIdsToPermissionNames = domainBusinessUnitUsers.getBusinessUnitUsers().stream()
            .collect(Collectors.toMap(
                BusinessUnitUser::getBusinessUnitId,
                buUser -> buUser.getPermissions().stream()
                    .map(this::toPermissionNameString)
                    .collect(Collectors.toSet())
            ));

        this.permissionCodes = domainBusinessUnitUsers.getBusinessUnitUsers().stream()
            .flatMap(buUser -> buUser.getPermissions().stream())
            .map(Permission::getPermissionCode)
            .collect(Collectors.toSet());

        this.businessUnitIdsToPermissionCodes = domainBusinessUnitUsers.getBusinessUnitUsers().stream()
            .collect(Collectors.toMap(
                BusinessUnitUser::getBusinessUnitId,
                buUser -> buUser.getPermissions().stream()
                    .map(Permission::getPermissionCode)
                    .collect(Collectors.toSet())
            ));
    }

    /**
     * For removal.
     *
     * @deprecated Use permission codes instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    public String toPermissionNameString(PermissionDescriptor permissionDescriptor) {
        return permissionDescriptor.getDescription()
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
        return businessUnitIdsToPermissionCodes
            .containsKey(businessUnitId);
    }

    /**
     * For removal.
     *
     * @deprecated Use hasPermissionCode(String permissionCode) instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    public boolean hasPermission(String permission) {
        return permissionNames.contains(permission);
    }

    /**
     * For removal.
     *
     * @deprecated Use hasPermissionCode(String permissionCode) instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    public boolean hasPermission(PermissionDescriptor permission) {
        return hasPermission(toPermissionNameString(permission));
    }

    public boolean hasPermissionCode(String permissionCode) {
        return permissionCodes.contains(permissionCode);
    }

    /**
     * For removal.
     *
     * @deprecated Use hasAtLeastOneOfPermissionCode(String... permissions) instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    public boolean hasAtLeastOneOfPermission(String... permissions) {
        return Arrays.stream(permissions).anyMatch(this::hasPermission);
    }

    /**
     * For removal.
     *
     * @deprecated Use hasAtLeastOneOfPermissionCode(String... permissions) instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    public boolean hasAtLeastOneOfPermission(PermissionDescriptor... permissions) {
        return Arrays.stream(permissions).anyMatch(this::hasPermission);
    }

    public boolean hasAtLeastOneOfPermissionCode(String... permissions) {
        return Arrays.stream(permissions).anyMatch(this::hasPermissionCode);
    }

    /**
     * For removal.
     *
     * @deprecated Use hasPermissionCodeInBusinessUnit(String permission, Short businessUnitId) instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    public boolean hasPermissionInBusinessUnit(String permission, Short businessUnitId) {
        List<String> permissionsInBusinessUnit = businessUnitIdsToPermissionNames
            .getOrDefault(businessUnitId, Set.of())
            .stream()
            .toList();
        return permissionsInBusinessUnit.contains(permission);
    }

    /**
     * For removal.
     *
     * @deprecated Use hasPermissionCodeInBusinessUnit(String permission, Short businessUnitId) instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    public boolean hasPermissionInBusinessUnit(PermissionDescriptor permission, Short businessUnitId) {
        return hasPermissionInBusinessUnit(toPermissionNameString(permission), businessUnitId);
    }

    public boolean hasPermissionCodeInBusinessUnit(String permission, Short businessUnitId) {
        List<String> permissionsInBusinessUnit = businessUnitIdsToPermissionCodes
            .getOrDefault(businessUnitId, Set.of())
            .stream()
            .toList();
        return permissionsInBusinessUnit.contains(permission);
    }

    /**
     * For removal.
     *
     * @deprecated Use hasAtLeastOneOfPermissionCodeInBusinessUnit(Short businessUnitId, String... permissions) instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    public boolean hasAtLeastOneOfPermissionInBusinessUnit(Short businessUnitId, PermissionDescriptor... permissions) {
        return Arrays.stream(permissions).anyMatch(p -> hasPermissionInBusinessUnit(p, businessUnitId));
    }

    public boolean hasAtLeastOneOfPermissionInBusinessUnit(Short businessUnitId, String... permissions) {
        return Arrays.stream(permissions).anyMatch(p -> hasPermissionInBusinessUnit(p, businessUnitId));
    }

    /**
     * For removal.
     *
     * @deprecated Use hasAtLeastOneOfPermissionCodeInBusinessUnit(Short businessUnitId, String... permissions) instead.
     */
    @Deprecated(since = "21/09/2026", forRemoval = true)
    public boolean hasAtLeastOneOfPermissionCodeInBusinessUnit(Short businessUnitId, String... permissions) {
        return Arrays.stream(permissions).anyMatch(p -> hasPermissionCodeInBusinessUnit(p, businessUnitId));
    }
}
