package uk.gov.hmcts.opal.common.user.authorisation.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserStateTest {

    @Test
    void getDisplayName_returnsDisplayNameWhenPresent() {
        UserState userState = UserState.builder()
            .userId(1L)
            .userName("normal@users.com")
            .name("Normal User")
            .businessUnitUser(Set.of())
            .build();

        assertEquals("Normal User", userState.getDisplayName());
    }

    @Test
    void getDisplayName_fallsBackToUsernameWhenDisplayNameMissing() {
        UserState userState = UserState.builder()
            .userId(1L)
            .userName("normal@users.com")
            .name(" ")
            .businessUnitUser(Set.of())
            .build();

        assertEquals("normal@users.com", userState.getDisplayName());
    }
}
