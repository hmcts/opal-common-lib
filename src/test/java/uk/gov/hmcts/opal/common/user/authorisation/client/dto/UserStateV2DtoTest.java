package uk.gov.hmcts.opal.common.user.authorisation.client.dto;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.common.dto.ToJsonString;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class UserStateV2DtoTest {

    @Test
    void getVersion_returnsBigIntegerWhenVersionIsPresent() {
        UserStateV2Dto dto = UserStateV2Dto.builder()
            .version(123L)
            .build();

        assertThat(dto.getVersion()).isEqualTo(BigInteger.valueOf(123L));
    }

    @Test
    void getVersion_returnsNullWhenVersionIsAbsent() {
        UserStateV2Dto dto = UserStateV2Dto.builder()
            .version(null)
            .build();

        assertThat(dto.getVersion()).isNull();
    }

    @Test
    void deserializesDomainsUsingDisplayNameKeys() throws Exception {
        String json = """
            {
              "domains": {
                "fines": {
                  "business_unit_users": []
                }
              }
            }
            """;

        UserStateV2Dto dto = ToJsonString.getObjectMapper().readValue(json, UserStateV2Dto.class);

        assertThat(dto.getDomains()).containsOnlyKeys(Domain.FINES);
    }
}
