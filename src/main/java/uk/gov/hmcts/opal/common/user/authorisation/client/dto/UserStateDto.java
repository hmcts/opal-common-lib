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
public class UserStateDto implements Versioned, ToJsonString {

    @JsonProperty("user_id")
    private Long userId;

    //users.username
    @JsonProperty("username")
    private String username;

    //Obtained from the Access Token (via Spring Security) until stored in the Database under
    // TDIA: User Service - Matching Key and JIT Provisioning, and only applies when id is 0 (zero) until then.
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
