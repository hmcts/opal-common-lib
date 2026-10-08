package uk.gov.hmcts.opal.common.user.authorisation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessUnitUserV2Dto {

    @JsonProperty("business_unit_user_id")
    private String businessUnitUserId;

    @JsonProperty("business_unit_id")
    private Short businessUnitId;

    @JsonProperty("permissions")
    private List<PermissionV2Dto> permissions;
}
