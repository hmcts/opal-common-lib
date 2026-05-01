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
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStatus;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            () -> assertTrue(authenticationToken.hasBusinessUnit("303")),
            () -> assertTrue(authenticationToken.hasPermissionInBusinessUnit("PERM_D", "303"))
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
            () -> assertFalse(authenticationToken.hasBusinessUnit("101")),
            () -> assertFalse(authenticationToken.hasPermissionInBusinessUnit("PERM_A", "303")),
            () -> assertFalse(authenticationToken.hasPermission("PERM_A"))
        );
    }

    @Test
    void hasBusinessUnitShouldReturnTrueWhenPresentAndFalseWhenMissing() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean hasExistingBusinessUnit = authenticationToken.hasBusinessUnit("101");
        boolean hasMissingBusinessUnit = authenticationToken.hasBusinessUnit("999");

        //Assert
        assertAll("business unit presence checks",
            () -> assertTrue(hasExistingBusinessUnit),
            () -> assertFalse(hasMissingBusinessUnit)
        );
    }

    @Test
    void hasBusinessUnitShouldThrowWhenBusinessUnitIdIsNotNumeric() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        NumberFormatException exception = assertThrows(
            NumberFormatException.class,
            () -> authenticationToken.hasBusinessUnit("not-a-number")
        );

        //Assert
        assertTrue(exception.getMessage().equals("For input string: \"not-a-number\""));
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
        boolean hasPermissionInBusinessUnit = authenticationToken.hasPermissionInBusinessUnit("PERM_B", "101");

        //Assert
        assertTrue(hasPermissionInBusinessUnit);
    }

    @Test
    void hasPermissionInBusinessUnitShouldReturnFalseWhenPermissionOrBusinessUnitMissing() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        boolean missingPermissionInExistingBusinessUnit =
            authenticationToken.hasPermissionInBusinessUnit("PERM_C", "101");
        boolean missingBusinessUnit = authenticationToken.hasPermissionInBusinessUnit("PERM_A", "999");

        //Assert
        assertAll("permission-in-business-unit negative checks",
            () -> assertFalse(missingPermissionInExistingBusinessUnit),
            () -> assertFalse(missingBusinessUnit)
        );
    }

    @Test
    void hasPermissionInBusinessUnitShouldThrowWhenBusinessUnitIdIsNotNumeric() {
        // Arrange
        OpalJwtAuthenticationToken authenticationToken = createToken();

        // Act
        NumberFormatException exception = assertThrows(
            NumberFormatException.class,
            () -> authenticationToken.hasPermissionInBusinessUnit("PERM_A", "invalid-id")
        );

        //Assert
        assertTrue(exception.getMessage().contains("For input string"));
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
}
