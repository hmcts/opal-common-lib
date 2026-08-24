package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2.DeveloperBusinessUnitUserV2;

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
    Map<Domain, DomainBusinessUnitUsers> domains;

    @JsonCreator
    public UserStateV2(
        @JsonProperty("user_id") @NonNull Long userId,
        @JsonProperty("username") @NonNull String username,
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

    public boolean anyBusinessUnitUserHasPermission(PermissionDescriptorV2 permission) {
        for (DomainBusinessUnitUsers domainBusinessUnitUsers : getDomains().values()) {
            boolean hasPermission = domainBusinessUnitUsers.anyBusinessUnitUserHasPermission(permission);

            if (hasPermission) {
                return true;
            }
        }

        return false;
    }

    public boolean anyBusinessUnitUserHasAnyPermission(PermissionDescriptorV2... permission) {
        for (DomainBusinessUnitUsers domainBusinessUnitUsers : getDomains().values()) {
            boolean hasPermission = domainBusinessUnitUsers.anyBusinessUnitUserHasAnyPermission(permission);
            if (hasPermission) {
                return true;
            }
        }

        return false;
    }

    public boolean hasBusinessUnitUserWithAnyPermission(short businessUnitId, PermissionDescriptorV2... permissions) {
        return userHasAnyPermission(getBusinessUnitUserForBusinessUnit(businessUnitId), permissions);
    }

    public static boolean userHasAnyPermission(Optional<BusinessUnitUserV2> user,
        PermissionDescriptorV2... permissions) {
        return user.stream().anyMatch(r -> r.hasAnyPermission(permissions));
    }

    public Optional<BusinessUnitUserV2> getBusinessUnitUserForBusinessUnit(short businessUnitId) {
        for (DomainBusinessUnitUsers domainBusinessUnitUsers : getDomains().values()) {
            Optional<BusinessUnitUserV2> hasBusinessUnit = domainBusinessUnitUsers.businessUnitUsers.stream()
                .filter(r -> r.matchesBusinessUnitId(businessUnitId))
                .findFirst();

            if (hasBusinessUnit.isPresent()) {
                return hasBusinessUnit;
            }
        }

        return Optional.empty();
    }


    public boolean hasBusinessUnitUserWithPermission(short businessUnitId, PermissionDescriptorV2 permission) {
        return userHasPermission(getBusinessUnitUserForBusinessUnit(businessUnitId), permission);
    }

    public static boolean userHasPermission(Optional<BusinessUnitUserV2> user, PermissionDescriptorV2 permission) {
        return user.stream().anyMatch(r -> r.hasPermission(permission));
    }

    public boolean noBusinessUnitUserHasPermission(PermissionDescriptorV2 permission) {
        return !anyBusinessUnitUserHasPermission(permission);
    }

    public UserStateV2.UserBusinessUnits allBusinessUnitUsersWithPermission(PermissionDescriptorV2 permission) {
        Set<BusinessUnitUserV2> businessUnitUsers = new HashSet<>();

        // A bit clunkier than a stream, but I understand how this works!
        for (DomainBusinessUnitUsers domainBusinessUnitUsers : domains.values()) {
            businessUnitUsers.addAll(domainBusinessUnitUsers.getBusinessUnitUsers()
                .stream().filter(r -> r.hasPermission(permission)).collect(Collectors.toSet()));
        }
        return new UserStateV2.UserBusinessUnitsImpl(businessUnitUsers);
    }

    public interface UserBusinessUnits {
        boolean containsBusinessUnit(Short businessUnitId);
    }

    public static class UserBusinessUnitsImpl implements UserStateV2.UserBusinessUnits {
        private final Set<BusinessUnitUserV2> businessUnitUser;
        private final Set<Short> businessUnits;

        public UserBusinessUnitsImpl(Set<BusinessUnitUserV2> businessUnitUser) {
            this.businessUnitUser = businessUnitUser;

            businessUnits = businessUnitUser.stream().map(BusinessUnitUserV2::getBusinessUnitId)
                .collect(Collectors.toSet());
        }

        public boolean containsBusinessUnit(Short businessUnitId) {
            return businessUnits.contains(businessUnitId);
        }
    }

    public static class DeveloperUserState extends UserStateV2 {
        private static final Optional<BusinessUnitUserV2> DEV_BUSINESS_UNIT_USER =
            Optional.of(new DeveloperBusinessUnitUserV2());

        public DeveloperUserState() {
            super(0L, "Developer_User", "Developer User", UserStatus.ACTIVE, 0L,
                "Unknown", Collections.emptyMap());
        }

        @Override
        public boolean anyBusinessUnitUserHasPermission(PermissionDescriptorV2 permission) {
            return true;
        }

        public boolean anyBusinessUnitUserHasAnyPermission(PermissionDescriptorV2... permission) {
            return true;
        }

        @Override
        public boolean hasBusinessUnitUserWithAnyPermission(short businessUnitId,
            PermissionDescriptorV2... permissions) {
            return true;
        }

        @Override
        public Optional<BusinessUnitUserV2> getBusinessUnitUserForBusinessUnit(short businessUnitId) {
            return DEV_BUSINESS_UNIT_USER;
        }

        @Override
        public boolean hasBusinessUnitUserWithPermission(short businessUnitId, PermissionDescriptorV2 permission) {
            return true;
        }


        @Override
        public UserBusinessUnits allBusinessUnitUsersWithPermission(PermissionDescriptorV2 permission) {
            return new UserBusinessUnits() {
                @Override
                public boolean containsBusinessUnit(Short businessUnitId) {
                    return true;
                }

            };
        }
    }

}
