package uk.gov.hmcts.opal.common.user.authorisation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainDto {

    @JsonProperty("business_unit_users")
    private List<BusinessUnitUserDto> businessUnitUsers;
}
