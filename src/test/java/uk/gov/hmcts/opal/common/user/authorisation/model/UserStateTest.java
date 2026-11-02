package uk.gov.hmcts.opal.common.user.authorisation.model;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserStateTest {

    @Test
    void getDisplayName_returnsNameWhenPresent() {
        UserStateV2 userState = UserStateV2.builder()
            .userId(1L)
            .username("normal@users.com")
            .name("Normal User")
            .domains(Map.of())
            .build();

        assertEquals("Normal User", userState.getName());
    }

    @Test
    void getDisplayName_fallsBackToUsernameWhenNameMissing() {
        UserStateV2 userState = UserStateV2.builder()
            .userId(1L)
            .username("normal@users.com")
            .name(" ")
            .domains(Map.of())
            .build();

        assertEquals("normal@users.com", userState.getUsername());
    }
}
