package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Builder
@Data
public class BusinessUnitUserV2 {

    @JsonProperty("business_unit_user_id")
    @NonNull
    String businessUnitUserId;

    @JsonProperty("business_unit_id")
    @NonNull
    Short businessUnitId;

    @JsonProperty("permissions")
    @EqualsAndHashCode.Exclude
    @NonNull
    Set<PermissionV2> permissions;

    @JsonCreator
    public BusinessUnitUserV2(@JsonProperty("business_unit_user_id") @NonNull String businessUnitUserId,
        @JsonProperty("business_unit_id") @NonNull Short businessUnitId,
        @JsonProperty("permissions") @NonNull Set<PermissionV2> permissions) {

        this.businessUnitUserId = businessUnitUserId;
        this.businessUnitId = businessUnitId;
        this.permissions = permissions;
    }

    public boolean hasPermission(PermissionDescriptorV2 reqPermission) {
        return permissions.stream().anyMatch(p -> p.matchesPermissions(reqPermission));
    }

    public boolean hasAnyPermission(PermissionDescriptorV2... reqPermissions) {
        return Arrays.stream(reqPermissions).anyMatch(this::hasPermission);
    }

    public boolean doesNotHavePermission(PermissionDescriptorV2 permission) {
        return !hasPermission(permission);
    }

    public boolean doesNotHaveAnyPermission(PermissionDescriptorV2... reqPermissions) {
        return !hasAnyPermission(reqPermissions);
    }

    public boolean matchesBusinessUnitId(Short businessUnitId) {
        return this.businessUnitId.equals(businessUnitId);
    }

    public boolean matchesBusinessUnitId(Collection<Short> businessUnitIds) {
        return businessUnitIds.contains(this.businessUnitId);
    }

}
