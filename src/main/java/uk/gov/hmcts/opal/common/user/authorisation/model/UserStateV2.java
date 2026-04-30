package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.io.Serializable;
import java.util.Map;

@Builder
@Data
public class UserStateV2 implements Serializable {

    @JsonProperty("user_id")
    @NonNull
    Long userId;

    @JsonProperty("username")
    @NonNull
    String username;

    @JsonProperty("name")
    String name;

    @JsonProperty("status")
    String status;

    @JsonProperty("version")
    Long version;

    @JsonProperty("cache_name")
    String cacheName;

    @JsonProperty("domains")
    @EqualsAndHashCode.Exclude
    Map<Domain, DomainBusinessUnitUsers> domains;

    @JsonCreator
    public UserStateV2(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("username") String username,
        @JsonProperty("name") String name,
        @JsonProperty("status") String status,
        @JsonProperty("version") Long version,
        @JsonProperty("cache_name") String cacheName,
        @JsonProperty("domains") Map<Domain, DomainBusinessUnitUsers> domains
    ) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.status = status;
        this.version = version;
        this.cacheName = cacheName;
        this.domains = domains;
    }

    public DomainBusinessUnitUsers getDomainBusinessUnitUsers(Domain domain) {
        return domains.get(domain);
    }
}
