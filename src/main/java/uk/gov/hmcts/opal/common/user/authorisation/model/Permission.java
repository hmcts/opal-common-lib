package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class Permission implements PermissionDescriptor {

    @JsonProperty("permission_id")
    @NonNull
    Long permissionId;

    @JsonProperty("permission_name")
    @NonNull
    String permissionName;

    @JsonProperty("permission_code")
    @NotNull
    String permissionCode;

    @JsonCreator
    public Permission(@JsonProperty("permission_id") Long permissionId,
        @JsonProperty("permission_name") String permissionName,
        @JsonProperty("permission_code") String permissionCode) {
        this.permissionId = permissionId;
        this.permissionName = permissionName;
        this.permissionCode = permissionCode;
    }

    boolean matchesPermissions(PermissionDescriptor candidate) {
        return candidate.getId() == permissionId;
    }

    @Override
    public long getId() {
        return permissionId;
    }

    @Override
    public String getDescription() {
        return permissionName;
    }
}
