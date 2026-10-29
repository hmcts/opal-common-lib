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
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2.UserBusinessUnits;

@Builder
@Data
public class DomainBusinessUnitUsers {

    @JsonProperty("business_unit_users")
    List<BusinessUnitUserV2> businessUnitUsers;

    @JsonCreator
    public DomainBusinessUnitUsers(@JsonProperty("business_unit_users") List<BusinessUnitUserV2> businessUnitUsers) {
        this.businessUnitUsers = businessUnitUsers;
    }

    public Optional<BusinessUnitUserV2> getBusinessUnitUserForBusinessUnit(short businessUnitId) {
        return businessUnitUsers.stream()
            .filter(r -> r.matchesBusinessUnitId(businessUnitId))
            .findFirst();
    }

    public boolean anyBusinessUnitUserHasPermission(PermissionDescriptorV2 permission) {
        return businessUnitUsers.stream().anyMatch(r -> r.hasPermission(permission));
    }

    public boolean noBusinessUnitUserHasPermission(PermissionDescriptorV2 permission) {
        return !anyBusinessUnitUserHasPermission(permission);
    }

    public boolean anyBusinessUnitUserHasAnyPermission(PermissionDescriptorV2... permissions) {
        return businessUnitUsers.stream().anyMatch(r -> r.hasAnyPermission(permissions));
    }

    public UserBusinessUnits allBusinessUnitUsersWithPermission(PermissionDescriptorV2 permission) {
        return new UserBusinessUnitsImpl(
            businessUnitUsers.stream().filter(r -> r.hasPermission(permission)).collect(Collectors.toSet()));
    }

    public boolean hasBusinessUnitUserWithPermission(short businessUnitId, PermissionDescriptorV2 permission) {
        return userHasPermission(getBusinessUnitUserForBusinessUnit(businessUnitId), permission);
    }

    public static boolean userHasPermission(Optional<BusinessUnitUserV2> user, PermissionDescriptorV2 permission) {
        return user.stream().anyMatch(r -> r.hasPermission(permission));
    }

    public boolean hasBusinessUnitUserWithAnyPermission(short businessUnitId, PermissionDescriptorV2... permissions) {
        return userHasAnyPermission(getBusinessUnitUserForBusinessUnit(businessUnitId), permissions);
    }

    public static boolean userHasAnyPermission(Optional<BusinessUnitUserV2> user,
                        PermissionDescriptorV2... permissions) {
        return user.stream().anyMatch(r -> r.hasAnyPermission(permissions));
    }

    public Set<Short> filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions(
        Optional<List<Short>> businessUnitIds, PermissionDescriptorV2... permissions) {

        return filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions(
            businessUnitIds.orElse(Collections.emptyList()), permissions);
    }

    public Set<Short> filterBusinessUnitsByBusinessUnitUsersWithAnyPermissions(
        List<Short> businessUnitIds, PermissionDescriptorV2... permissions) {

        return businessUnitIds.stream()
            .filter(buid -> hasBusinessUnitUserWithAnyPermission(buid, permissions))
            .collect(Collectors.toSet());
    }

    public static class UserBusinessUnitsImpl implements UserBusinessUnits {
        private final Set<Short> businessUnitIds;

        public UserBusinessUnitsImpl(Set<BusinessUnitUserV2> businessUnitUsers) {
            businessUnitIds = businessUnitUsers.stream().map(r -> r.getBusinessUnitId())
                .collect(Collectors.toSet());
        }

        public boolean containsBusinessUnit(Short businessUnitId) {
            return businessUnitIds.contains(businessUnitId);
        }
    }
}
