package uk.gov.hmcts.opal.common.user.authorisation.model;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

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
    Map<Domain, DomainBusinessUnitUsers> domains;

    @JsonCreator
    public UserStateV2(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("username") String username,
        @JsonProperty("name") String name,
        @JsonProperty("status") UserStatus status,
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

    public Map<Domain, DomainBusinessUnitUsers> getDomains() {
        if (domains == null) {
            return new HashMap<>();
        }
        return domains;
    }

    public DomainBusinessUnitUsers getDomainBusinessUnitUsers(Domain domain) {
        return (domain != null && getDomains().containsKey(domain) && getDomains().get(domain) != null)
            ?
            domains.get(domain) :
            DomainBusinessUnitUsers.builder().businessUnitUsers(emptyList()).build();
    }

    public boolean isBusinessUnitPermittedForCurrentUser(Short businessUnitId, Domain domain) {
        if (businessUnitId == null) {
            return false;
        }

        return getAllBusinessUnitUsersForCurrentUser(domain).stream()
            .map(BusinessUnitUser::getBusinessUnitId)
            .anyMatch(id -> id.equals(businessUnitId));
    }

    public boolean checkAnyBusinessUnitUserHasPermission(Permission permission, Domain domain) {

        return permission != null && getDomainBusinessUnitUsers(domain).getBusinessUnitUsers().stream()
            .anyMatch(buUser -> buUser.getPermissions().contains(permission));
    }

    public List<BusinessUnitUser> getAllBusinessUnitUsersForCurrentUser(Domain domain) {

        DomainBusinessUnitUsers domainBusinessUnitUsers = getDomainBusinessUnitUsers(domain);

        if (domainBusinessUnitUsers == null
            || domainBusinessUnitUsers.getBusinessUnitUsers() == null
            || domainBusinessUnitUsers.getBusinessUnitUsers().isEmpty()) {
            return List.of();
        }

        return domainBusinessUnitUsers.getBusinessUnitUsers();
    }

    public List<BusinessUnitUser> getBusinessUnitUsersForBusinessUnitIds(List<Long> businessUnitIds, Domain domain) {
        if (businessUnitIds == null || businessUnitIds.isEmpty()) {
            return List.of();
        }

        return getAllBusinessUnitUsersForCurrentUser(domain).stream()
            .filter(buUser -> businessUnitIds.contains(buUser.getBusinessUnitId().longValue()))
            .toList();
    }


}
