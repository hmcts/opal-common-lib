package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
@Data
public class DomainBusinessUnitUsers {

    @JsonProperty("business_unit_users")
    List<BusinessUnitUser> businessUnitUsers;

    @JsonCreator
    public DomainBusinessUnitUsers(@JsonProperty("business_unit_users") List<BusinessUnitUser> businessUnitUsers) {
        this.businessUnitUsers = businessUnitUsers;
    }

    public Optional<BusinessUnitUser> getBusinessUnitUserForBusinessUnit(short businessUnitId) {
        return businessUnitUsers.stream()
            .filter(r -> r.matchesBusinessUnitId(businessUnitId))
            .findFirst();
    }

    public boolean anyBusinessUnitUserHasPermission(PermissionDescriptor permission) {
        return businessUnitUsers.stream().anyMatch(r -> r.hasPermission(permission));
    }

    public boolean noBusinessUnitUserHasPermission(PermissionDescriptor permission) {
        return !anyBusinessUnitUserHasPermission(permission);
    }

    public boolean anyBusinessUnitUserHasAnyPermission(PermissionDescriptor... permissions) {
        return businessUnitUsers.stream().anyMatch(r -> r.hasAnyPermission(permissions));
    }

    public UserBusinessUnits allBusinessUnitUsersWithPermission(PermissionDescriptor permission) {
        return new UserBusinessUnitsImpl(
            businessUnitUsers.stream().filter(r -> r.hasPermission(permission)).collect(Collectors.toSet()));
    }

    public boolean hasBusinessUnitUserWithPermission(short businessUnitId, PermissionDescriptor permission) {
        return userHasPermission(getBusinessUnitUserForBusinessUnit(businessUnitId), permission);
    }

    public static boolean userHasPermission(Optional<BusinessUnitUser> user, PermissionDescriptor permission) {
        return user.stream().anyMatch(r -> r.hasPermission(permission));
    }

    public boolean hasBusinessUnitUserWithAnyPermission(short businessUnitId, PermissionDescriptor... permissions) {
        return userHasAnyPermission(getBusinessUnitUserForBusinessUnit(businessUnitId), permissions);
    }

    public static boolean userHasAnyPermission(Optional<BusinessUnitUser> user, PermissionDescriptor... permissions) {
        return user.stream().anyMatch(r -> r.hasAnyPermission(permissions));
    }

    public Set<Short> filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions(
        Optional<List<Short>> businessUnitIds, PermissionDescriptor... permissions) {

        return filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions(
            businessUnitIds.orElse(Collections.emptyList()), permissions);
    }

    public Set<Short> filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions(
        List<Short> businessUnitIds, PermissionDescriptor... permissions) {

        return businessUnitIds.stream()
            .filter(buid -> hasBusinessUnitUserWithAnyPermission(buid, permissions))
            .collect(Collectors.toSet());
    }

    public interface UserBusinessUnits {
        boolean containsBusinessUnit(Short businessUnitId);
    }

    public static class UserBusinessUnitsImpl implements UserBusinessUnits {
        private final Set<Short> businessUnitIds;

        public UserBusinessUnitsImpl(Set<BusinessUnitUser> businessUnitUsers) {
            businessUnitIds = businessUnitUsers.stream().map(r -> r.getBusinessUnitId())
                .collect(Collectors.toSet());
        }

        public boolean containsBusinessUnit(Short businessUnitId) {
            return businessUnitIds.contains(businessUnitId);
        }
    }
}
