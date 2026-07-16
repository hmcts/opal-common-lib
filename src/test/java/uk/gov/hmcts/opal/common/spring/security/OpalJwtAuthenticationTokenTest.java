package uk.gov.hmcts.opal.common.spring.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsersV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptorV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpalJwtAuthenticationTokenTest {

    private static final String JWT_SUBJECT = "subject-123";

    @Test
    void constructorShouldExposeDataFromSpecifiedDomainWhenMultipleDomainsExist() {
        // Arrange
        Jwt jwt = createJwt(JWT_SUBJECT);
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // Act
        OpalJwtAuthenticationToken authenticationToken = new OpalJwtAuthenticationToken(
            createUserStateWithMultipleDomainData(),
            Domain.CONFISCATION,
            jwt,
            authorities,
            "request-details"
        );

        //Assert
        assertAll("confiscation domain data is exposed",
            () -> assertTrue(authenticationToken.hasPermission("ADD_ACCOUNT_ACTIVITY_NOTES")),
            () -> assertTrue(authenticationToken.hasBusinessUnit((short) 303)),
            () -> assertTrue(authenticationToken.hasPermissionInBusinessUnit("ADD_ACCOUNT_ACTIVITY_NOTES", (short) 303))
        );
    }

    @Test
    void constructorShouldHidePermissionsFromOtherDomainsWhenMultipleDomainsExist() {
        // Arrange
        Jwt jwt = createJwt(JWT_SUBJECT);
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // Act
        OpalJwtAuthenticationToken authenticationToken = new OpalJwtAuthenticationToken(
            createUserStateWithMultipleDomainData(),
            Domain.CONFISCATION,
            jwt,
            authorities,
            "request-details"
        );

        //Assert
        assertAll("other domain data is hidden",
            () -> assertFalse(authenticationToken.hasBusinessUnit((short) 101)),
            () -> assertFalse(authenticationToken.hasPermissionInBusinessUnit("PERM_A", (short) 303)),
            () -> assertFalse(authenticationToken.hasPermission("PERM_A"))
        );
    }

    @Test
    void hasBusinessUnitShouldReturnTrueWhenPresentAndFalseWhenMissing() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean hasExistingBusinessUnit = authenticationToken.hasBusinessUnit((short) 101);
        boolean hasMissingBusinessUnit = authenticationToken.hasBusinessUnit((short) 999);

        //Assert
        assertAll("business unit presence checks",
            () -> assertTrue(hasExistingBusinessUnit),
            () -> assertFalse(hasMissingBusinessUnit)
        );
    }

    @Test
    void hasPermissionShouldReturnTrueWhenPermissionExistsAndFalseWhenMissing() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean hasPermission = authenticationToken.hasPermission("ACCOUNT_ENQUIRY");
        boolean hasMissingPermission = authenticationToken.hasPermission("UNKNOWN_PERMISSION");

        //Assert
        assertAll("permission presence checks",
            () -> assertTrue(hasPermission),
            () -> assertFalse(hasMissingPermission)
        );
    }

    @Test
    void hasPermissionInBusinessUnitShouldReturnTrueWhenPermissionExists() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean hasPermissionInBusinessUnit = authenticationToken.hasPermissionInBusinessUnit("ACCOUNT_ENQUIRY",
            (short) 101);

        //Assert
        assertTrue(hasPermissionInBusinessUnit);
    }

    @Test
    void hasPermissionInBusinessUnitShouldReturnFalseWhenPermissionOrBusinessUnitMissing() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean missingPermissionInExistingBusinessUnit =
            authenticationToken.hasPermissionInBusinessUnit("PERM_C", (short) 101);
        boolean missingBusinessUnit = authenticationToken.hasPermissionInBusinessUnit("PERM_A", (short) 999);

        //Assert
        assertAll("permission-in-business-unit negative checks",
            () -> assertFalse(missingPermissionInExistingBusinessUnit),
            () -> assertFalse(missingBusinessUnit)
        );
    }

    private OpalJwtAuthenticationToken createToken() {
        return new OpalJwtAuthenticationToken(
            createUserStateWithMultipleDomainData(),
            Domain.FINES,
            createJwt(JWT_SUBJECT),
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            "details"
        );
    }

    private Jwt createJwt(String subject) {
        return new Jwt(
            "token-value",
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T01:00:00Z"),
            Map.of("alg", "none"),
            Map.of(JwtClaimNames.SUB, subject)
        );
    }

    private UserStateV2 createUserStateWithMultipleDomainData() {
        PermissionV2 permA = PermissionV2.ACCOUNT_ENQUIRY;

        PermissionV2 permB = PermissionV2.ACCOUNT_ENQUIRY_NOTES;

        PermissionV2 permC = PermissionV2.ACCOUNT_MAINTENANCE;

        PermissionV2 permD = PermissionV2.ADD_ACCOUNT_ACTIVITY_NOTES;

        BusinessUnitUserV2 businessUnit101 = BusinessUnitUserV2.builder()
            .businessUnitUserId("bu-user-101")
            .businessUnitId((short) 101)
            .permissions(Set.of(permA, permB))
            .build();

        BusinessUnitUserV2 businessUnit202 = BusinessUnitUserV2.builder()
            .businessUnitUserId("bu-user-202")
            .businessUnitId((short) 202)
            .permissions(Set.of(permC))
            .build();

        BusinessUnitUserV2 businessUnit303 = BusinessUnitUserV2.builder()
            .businessUnitUserId("bu-user-303")
            .businessUnitId((short) 303)
            .permissions(Set.of(permD))
            .build();

        DomainBusinessUnitUsersV2 finesDomainBusinessUnitUsers = DomainBusinessUnitUsersV2.builder()
            .businessUnitUsers(List.of(businessUnit101, businessUnit202))
            .build();

        DomainBusinessUnitUsersV2 confiscationDomainBusinessUnitUsers = DomainBusinessUnitUsersV2.builder()
            .businessUnitUsers(List.of(businessUnit303))
            .build();

        return UserStateV2.builder()
            .userId(55L)
            .username("test.user")
            .name("Test User")
            .status(UserStatus.ACTIVE)
            .version(6L)
            .cacheName("user-state-cache")
            .domains(Map.of(
                Domain.FINES, finesDomainBusinessUnitUsers,
                Domain.CONFISCATION, confiscationDomainBusinessUnitUsers
            ))
            .build();
    }

    @Test
    void hasPermission_PermissionDescriptor_shouldReturnTheCorrectStatus() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean hasPermission = authenticationToken.hasPermission(PermissionV2.ACCOUNT_ENQUIRY);
        boolean hasMissingPermission = authenticationToken.hasPermission(PermissionV2.CONSOLIDATE);

        //Assert
        assertAll("permission presence checks",
            () -> assertTrue(hasPermission),
            () -> assertFalse(hasMissingPermission)
        );
    }

    @Test
    void hasAtLeastOneOfPermission_PermissionDescriptor_shouldReturnTheCorrectStatus() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();
        // Act
        //  Note that 'CONSOLIDATE' and 'CREATE_MANAGE_DRAFT_ACCOUNTS' are not used in the toke creation.
        //  We need to use 'real' permission values now.
        boolean hasPermission = authenticationToken.hasAtLeastOneOfPermission(
            PermissionV2.ACCOUNT_ENQUIRY,
            PermissionV2.CONSOLIDATE);

        boolean hasMissingPermission = authenticationToken.hasAtLeastOneOfPermission(
            PermissionV2.CONSOLIDATE,
            PermissionV2.CREATE_MANAGE_DRAFT_ACCOUNTS);

        //Assert
        assertAll("permission presence checks",
            () -> assertTrue(hasPermission),
            () -> assertFalse(hasMissingPermission)
        );
    }

    @Test
    void hasAtLeastOneOfPermission_String_shouldReturnTheCorrectStatus() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean hasPermission = authenticationToken.hasAtLeastOneOfPermission(
            "ACCOUNT_ENQUIRY",
                        "CONSOLIDATE");

        boolean hasMissingPermission = authenticationToken.hasAtLeastOneOfPermission(
            "CONSOLIDATE",
                        "CREATE_MANAGE_DRAFT_ACCOUNTS");

        //Assert
        assertAll("permission presence checks",
            () -> assertTrue(hasPermission),
            () -> assertFalse(hasMissingPermission)
        );
    }


    @Test
    void hasPermissionInBusinessUnit_PermissionDescriptor_ShouldReturnTrueWhenPermissionExists() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean hasPermissionInBusinessUnit =
            authenticationToken.hasPermissionInBusinessUnit(PermissionV2.ACCOUNT_ENQUIRY_NOTES,
                (short) 101);

        //Assert
        assertTrue(hasPermissionInBusinessUnit);
    }

    @Test
    void hasPermissionInBusinessUnit_PermissionDescriptor_ShouldReturnFalseWhenPermissionOrBusinessUnitMissing() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean missingPermissionInExistingBusinessUnit =
            authenticationToken.hasPermissionInBusinessUnit(PermissionV2.CONSOLIDATE, (short) 101);
        boolean missingBusinessUnit =
            authenticationToken.hasPermissionInBusinessUnit(PermissionV2.ACCOUNT_ENQUIRY_NOTES, (short) 999);

        //Assert
        assertAll("permission-in-business-unit negative checks",
            () -> assertFalse(missingPermissionInExistingBusinessUnit),
            () -> assertFalse(missingBusinessUnit)
        );
    }


    @Test
    void hasAtLeastOneOfPermissionInBusinessUnit_PermissionDescriptor_ShouldReturnTrueWhenPermissionExists() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean hasPermissionInBusinessUnit =
            authenticationToken.hasAtLeastOneOfPermissionInBusinessUnit(
                (short) 101,
                PermissionV2.ACCOUNT_ENQUIRY,
                PermissionV2.CONSOLIDATE);
        //Assert
        assertTrue(hasPermissionInBusinessUnit);
    }

    @Test
    //This throws because of the method name but this is required to outline what the test is doing
    @SuppressWarnings("LineLength")
    void hasAtLeastOneOfPermissionInBusinessUnit_PermissionDescriptor_ShouldReturnFalseWhenPermissionOrBusinessUnitMissing() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean missingPermissionInExistingBusinessUnit =
            authenticationToken.hasAtLeastOneOfPermissionInBusinessUnit((short) 101,
                PermissionV2.AMEND_PAYMENT_TERMS,
                PermissionV2.CONSOLIDATE);
        boolean missingBusinessUnit =
            authenticationToken.hasAtLeastOneOfPermissionInBusinessUnit((short) 999,
                PermissionV2.ACCOUNT_ENQUIRY,
                PermissionV2.CONSOLIDATE);

        //Assert
        assertAll("permission-in-business-unit negative checks",
            () -> assertFalse(missingPermissionInExistingBusinessUnit),
            () -> assertFalse(missingBusinessUnit)
        );
    }



    @Test
    void hasAtLeastOneOfPermissionInBusinessUnit_String_ShouldReturnTrueWhenPermissionExists() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean hasPermissionInBusinessUnit =
            authenticationToken.hasAtLeastOneOfPermissionInBusinessUnit(
                (short) 101,
                "ACCOUNT_ENQUIRY", "CONSOLIDATE");
        //Assert
        assertTrue(hasPermissionInBusinessUnit);
    }

    @Test
    void hasAtLeastOneOfPermissionInBusinessUnit_String_ShouldReturnFalseWhenPermissionOrBusinessUnitMissing() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean missingPermissionInExistingBusinessUnit =
            authenticationToken.hasAtLeastOneOfPermissionInBusinessUnit((short) 101,
                "PERM_C",
                "PERM_NOT_USED_A");
        boolean missingBusinessUnit =
            authenticationToken.hasAtLeastOneOfPermissionInBusinessUnit((short) 999,
                "PERM_A",
                "PERM_NOT_USED_A");

        //Assert
        assertAll("permission-in-business-unit negative checks",
            () -> assertFalse(missingPermissionInExistingBusinessUnit),
            () -> assertFalse(missingBusinessUnit)
        );
    }

    enum TestPermissionDescriptor implements PermissionDescriptorV2 {
        PERM_A("PERM_A", "Perm A"),
        PERM_B("PERM_B", "Perm B"),
        PERM_C("PERM_C", "Perm C"),
        PERM_NOT_USED_A("PERM_NOT_USED_A", "Perm Not Used A"),
        PERM_NOT_USED_B("PERM_NOT_USED_B", "Perm Not Used B"),;

        private final String permissionCode;
        private final String permissionName;

        @Override
        public String getPermissionCode() {
            return permissionCode;
        }

        @Override
        public String getPermissionName() {
            return permissionName;
        }

        /**
         * Enumeration constructor.
         *
         * @param permissionCode                The enumeration code
         * @param permissionName                The enumeration name
         */
        private TestPermissionDescriptor(String permissionCode, String permissionName) {
            this.permissionCode = permissionCode;
            this.permissionName = permissionName;
        }

    }
}
