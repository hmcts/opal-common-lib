package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Builder
@Data
public class BusinessUnitUser {

    @JsonProperty("business_unit_user_id")
    @NonNull
    String businessUnitUserId;

    @JsonProperty("business_unit_id")
    @NonNull
    Short businessUnitId;

    @JsonProperty("permissions")
    @EqualsAndHashCode.Exclude
    @NonNull
    Set<Permission> permissions;

    @JsonCreator
    public BusinessUnitUser(@JsonProperty("business_unit_user_id") String businessUnitUserId,
                            @JsonProperty("business_unit_id") Short businessUnitId,
                            @JsonProperty("permissions") Set<Permission> permissions) {

        this.businessUnitUserId = businessUnitUserId;
        this.businessUnitId = businessUnitId;
        this.permissions = permissions;
    }

    public boolean hasPermission(PermissionDescriptor reqPermission) {
        return permissions.stream().anyMatch(p -> p.matchesPermissions(reqPermission));
    }

    public boolean hasAnyPermission(PermissionDescriptor... reqPermissions) {
        return Arrays.stream(reqPermissions).anyMatch(this::hasPermission);
    }

    public boolean doesNotHavePermission(PermissionDescriptor permission) {
        return !hasPermission(permission);
    }

    public boolean doesNotHaveAnyPermission(PermissionDescriptor... reqPermissions) {
        return !hasAnyPermission(reqPermissions);
    }

    public boolean matchesBusinessUnitId(Short businessUnitId) {
        return this.businessUnitId.equals(businessUnitId);
    }

    public boolean matchesBusinessUnitId(Collection<Short> businessUnitIds) {
        return businessUnitIds.contains(this.businessUnitId);
    }

    public static class DeveloperBusinessUnitUser extends BusinessUnitUser {
        DeveloperBusinessUnitUser() {
            super("", Short.MAX_VALUE, Collections.emptySet());
        }

        @Override
        public boolean hasPermission(PermissionDescriptor reqPermission) {
            return true;
        }

        @Override
        public boolean hasAnyPermission(PermissionDescriptor... reqPermissions) {
            return true;
        }

        @Override
        public boolean matchesBusinessUnitId(Short businessUnitId) {
            return true;
        }

        public boolean matchesBusinessUnitId(Collection<Short> businessUnitIds) {
            return true;
        }

    }
}
