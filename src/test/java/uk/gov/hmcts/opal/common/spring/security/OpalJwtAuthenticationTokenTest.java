package uk.gov.hmcts.opal.common.spring.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;
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
            () -> assertTrue(authenticationToken.hasPermission("PERM_D")),
            () -> assertTrue(authenticationToken.hasBusinessUnit((short) 303)),
            () -> assertTrue(authenticationToken.hasPermissionInBusinessUnit("PERM_D", (short) 303))
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
        boolean hasPermission = authenticationToken.hasPermission("PERM_A");
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
        boolean hasPermissionInBusinessUnit = authenticationToken.hasPermissionInBusinessUnit("PERM_B", (short) 101);

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
        Permission permA = Permission.builder()
            .permissionId(1L)
            .permissionName("PERM_A")
            .build();

        Permission permB = Permission.builder()
            .permissionId(2L)
            .permissionName("PERM_B")
            .build();

        Permission permC = Permission.builder()
            .permissionId(3L)
            .permissionName("PERM_C")
            .build();

        Permission permD = Permission.builder()
            .permissionId(4L)
            .permissionName("PERM_D")
            .build();

        BusinessUnitUser businessUnit101 = BusinessUnitUser.builder()
            .businessUnitUserId("bu-user-101")
            .businessUnitId((short) 101)
            .permissions(Set.of(permA, permB))
            .build();

        BusinessUnitUser businessUnit202 = BusinessUnitUser.builder()
            .businessUnitUserId("bu-user-202")
            .businessUnitId((short) 202)
            .permissions(Set.of(permC))
            .build();

        BusinessUnitUser businessUnit303 = BusinessUnitUser.builder()
            .businessUnitUserId("bu-user-303")
            .businessUnitId((short) 303)
            .permissions(Set.of(permD))
            .build();

        DomainBusinessUnitUsers finesDomainBusinessUnitUsers = DomainBusinessUnitUsers.builder()
            .businessUnitUsers(List.of(businessUnit101, businessUnit202))
            .build();

        DomainBusinessUnitUsers confiscationDomainBusinessUnitUsers = DomainBusinessUnitUsers.builder()
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
        boolean hasPermission = authenticationToken.hasPermission(TestPermissionDescriptor.PERM_A);
        boolean hasMissingPermission = authenticationToken.hasPermission(TestPermissionDescriptor.PERM_NOT_USED_A);

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
        boolean hasPermission = authenticationToken.hasAtLeastOneOfPermission(
            TestPermissionDescriptor.PERM_A,
            TestPermissionDescriptor.PERM_NOT_USED_A);

        boolean hasMissingPermission = authenticationToken.hasAtLeastOneOfPermission(
            TestPermissionDescriptor.PERM_NOT_USED_A,
            TestPermissionDescriptor.PERM_NOT_USED_B);

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
            "PERM_A",
            "PERM_NOT_USED_A");

        boolean hasMissingPermission = authenticationToken.hasAtLeastOneOfPermission(
            "PERM_NOT_USED_A",
            "PERM_NOT_USED_B");

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
            authenticationToken.hasPermissionInBusinessUnit(TestPermissionDescriptor.PERM_B,
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
            authenticationToken.hasPermissionInBusinessUnit(TestPermissionDescriptor.PERM_C, (short) 101);
        boolean missingBusinessUnit =
            authenticationToken.hasPermissionInBusinessUnit(TestPermissionDescriptor.PERM_A, (short) 999);

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
                TestPermissionDescriptor.PERM_B,
                TestPermissionDescriptor.PERM_NOT_USED_A);
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
                TestPermissionDescriptor.PERM_C,
                TestPermissionDescriptor.PERM_NOT_USED_A);
        boolean missingBusinessUnit =
            authenticationToken.hasAtLeastOneOfPermissionInBusinessUnit((short) 999,
                TestPermissionDescriptor.PERM_A,
                TestPermissionDescriptor.PERM_NOT_USED_A);

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
                "PERM_B",
                "PERM_NOT_USED_A");
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

    enum TestPermissionDescriptor implements PermissionDescriptor {
        PERM_A,
        PERM_B,
        PERM_C,
        PERM_NOT_USED_A,
        PERM_NOT_USED_B;

        @Override
        public long getId() {
            return ordinal();
        }

        @Override
        public String getDescription() {
            return name();
        }
    }
}
