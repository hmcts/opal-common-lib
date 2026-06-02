package uk.gov.hmcts.opal.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ToJsonStringTest {

    private record TestDto(String firstName, String surname, int numberOfFailedLoginAttempts) {}

    @Test
    void objectToJson_shouldProduceSnakeCase() {
        var testDto = new TestDto("Harry", "Smith", 25);

        String json = ToJsonString.objectToJson(testDto);

        assertThat(json).isEqualToIgnoringNewLines("""
                                                       {
                                                       "first_name":"Harry",
                                                       "surname":"Smith",
                                                       "number_of_failed_login_attempts":25
                                                       }
                                                       """);
    }
}
