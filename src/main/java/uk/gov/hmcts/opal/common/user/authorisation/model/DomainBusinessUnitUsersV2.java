package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DomainBusinessUnitUsersV2 {

    @JsonProperty("business_unit_users")
    List<BusinessUnitUserV2> businessUnitUsers;

    @JsonCreator
    public DomainBusinessUnitUsersV2(@JsonProperty("business_unit_users") List<BusinessUnitUserV2> businessUnitUsers) {
        this.businessUnitUsers = businessUnitUsers;
    }

    public Optional<BusinessUnitUserV2> getBusinessUnitUserForBusinessUnit(short businessUnitId) {
        return businessUnitUsers.stream()
            .filter(r -> r.matchesBusinessUnitId(businessUnitId))
            .findFirst();
    }
}
