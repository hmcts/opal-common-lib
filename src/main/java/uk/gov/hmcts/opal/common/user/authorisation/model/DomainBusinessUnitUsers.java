package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class DomainBusinessUnitUsers {

    @JsonProperty("business_unit_users")
    List<BusinessUnitUser> businessUnitUsers;

    @JsonCreator
    public DomainBusinessUnitUsers(@JsonProperty("business_unit_users") List<BusinessUnitUser> businessUnitUsers) {
        this.businessUnitUsers = businessUnitUsers;
    }
}
