package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class Permission {

    @JsonProperty("permission_id")
    @NonNull
    Long permissionId;

    @JsonProperty("permission_name")
    @NonNull
    String permissionName;

    @JsonCreator
    public Permission(@JsonProperty("permission_id") Long permissionId,
                      @JsonProperty("permission_name") String permissionName) {
        this.permissionId = permissionId;
        this.permissionName = permissionName;
    }

    boolean matchesPermissions(PermissionDescriptor candidate) {
        return candidate.getId() == permissionId;
    }
}
