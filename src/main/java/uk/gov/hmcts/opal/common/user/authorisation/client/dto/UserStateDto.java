package uk.gov.hmcts.opal.common.user.authorisation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.common.dto.ToJsonString;
import uk.gov.hmcts.opal.common.dto.Versioned;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Deprecated//Use UserStateDTOV2
public class UserStateDto implements Versioned, ToJsonString {

    @JsonProperty("user_id")
    private Long userId;

    //users.username
    @JsonProperty("username")
    private String username;

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

    @JsonProperty("version")
    private Long version;

    @JsonProperty("business_unit_users")
    private List<BusinessUnitUserDto> businessUnitUsers;

    @Override
    public BigInteger getVersion() {
        return Optional.ofNullable(version).map(BigInteger::valueOf).orElse(null);
    }
}
