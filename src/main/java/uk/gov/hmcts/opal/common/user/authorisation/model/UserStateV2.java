package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;

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
    UserStatus status;

    @JsonProperty("version")
    Long version;

    @JsonProperty("cache_name")
    String cacheName;

    @JsonProperty("domains")
    Map<Domain, DomainBusinessUnitUsersV2> domains;

    @JsonCreator
    public UserStateV2(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("username") String username,
        @JsonProperty("name") String name,
        @JsonProperty("status") UserStatus status,
        @JsonProperty("version") Long version,
        @JsonProperty("cache_name") String cacheName,
        @JsonProperty("domains") Map<Domain, DomainBusinessUnitUsersV2> domains
    ) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.status = status;
        this.version = version;
        this.cacheName = cacheName;
        this.domains = domains;
    }

    public Map<Domain, DomainBusinessUnitUsersV2> getDomains() {
        if (domains == null) {
            return new HashMap<>();
        }
        return domains;
    }

    public DomainBusinessUnitUsersV2 getDomainBusinessUnitUsers(Domain domain) {
        return (domain != null && getDomains().containsKey(domain) && getDomains().get(domain) != null)
            ?
            domains.get(domain) :
            DomainBusinessUnitUsersV2.builder().businessUnitUsers(emptyList()).build();
    }
}
