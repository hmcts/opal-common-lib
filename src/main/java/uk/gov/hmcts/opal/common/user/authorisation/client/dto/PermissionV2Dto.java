package uk.gov.hmcts.opal.common.user.authorisation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionV2Dto {

    //  Properties:
    @JsonProperty("permission_code")
    private String permissionCode;

    @JsonProperty("permission_name")
    private String permissionName;

}
