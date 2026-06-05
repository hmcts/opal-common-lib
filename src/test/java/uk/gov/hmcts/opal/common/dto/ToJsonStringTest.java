package uk.gov.hmcts.opal.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void objectToPrettyJson_shouldProduceSnakeCase() {
        var testDto = new TestDto("Harry", "Smith", 25);

        String json = ToJsonString.objectToPrettyJson(testDto);

        assertAll(
            () -> assertTrue(json.contains("first_name")),
            () -> assertTrue(json.contains("surname")),
            () -> assertTrue(json.contains("number_of_failed_login_attempts"))
        );
    }

    @Test
    void objectToPrettyJsonsString_shouldProduceSnakeCase() {
        var testDto = new TestDto("Harry", "Smith", 25);

        String json = ToJsonString.objectToPrettyJson(testDto);

        assertAll(
            () -> json.contains("first_name"),
            () -> json.contains("surname"),
            () -> json.contains("number_of_failed_login_attempts")
        );
    }

    @Test
    void toPrettyJsonsString_shouldNotAlterPropertyCase() {
        String sourceJson = """
            {"propOne":"some value","propTwo":"another value"}
            """;

        String json = ToJsonString.toPrettyJson(sourceJson);

        assertAll(
            () -> json.contains("propOne"),
            () -> json.contains("propTwo")
        );
    }

    @Test
    void objectToJson_doesNotThrowOnNullInput() {
        String json = ToJsonString.objectToJson(null);

        assertThat(json).isEqualTo("null");
    }

    @Test
    void objectToPrettyJson_doesNotThrowOnNullInput() {
        String json = ToJsonString.objectToPrettyJson(null);

        assertThat(json).isEqualTo("null");
    }
}
