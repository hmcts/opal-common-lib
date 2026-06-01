package uk.gov.hmcts.opal.common.user.authentication.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

@Builder
@Value
public class SecurityToken {

    @JsonProperty("access_token")
    String accessToken;
    @JsonProperty("user_state")
    UserStateV2 userState;
}
