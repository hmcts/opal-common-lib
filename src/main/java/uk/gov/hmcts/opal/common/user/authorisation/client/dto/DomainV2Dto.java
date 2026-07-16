package uk.gov.hmcts.opal.common.user.authorisation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainV2Dto {

    @JsonProperty("business_unit_users")
    private List<BusinessUnitUserV2Dto> businessUnitUsers;
}
