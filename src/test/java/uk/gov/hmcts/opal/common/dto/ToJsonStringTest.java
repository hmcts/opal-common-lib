package uk.gov.hmcts.opal.common.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.opal.common.user.authorisation.exception.JsonRuntimeException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ToJsonStringTest {

    private record TestDto(String firstName, String surname, int numberOfFailedLoginAttempts) implements ToJsonString {
    }


    @Test
    void objectToJson_shouldProduceSnakeCase() {
        var testDto = new TestDto("Harry", "Smith", 25);

        String json = testDto.toJson();

        assertThat(json).isEqualToIgnoringNewLines("""
            {
            "first_name":"Harry",
            "surname":"Smith",
            "number_of_failed_login_attempts":25
            }
            """);
    }

    @Test
    void toPrettyJson_shouldProduceSnakeCase() {
        var testDto = new TestDto("Harry", "Smith", 25);

        String json = testDto.toPrettyJson();

        assertAll(
            () -> assertTrue(json.contains("first_name")),
            () -> assertTrue(json.contains("surname")),
            () -> assertTrue(json.contains("number_of_failed_login_attempts"))
        );
    }

    @Test
    void toPrettyJsonsString_shouldNotAlterPropertyCase() {
        String sourceJson = """
            {"propOne":"some value","propTwo":"another value"}
            """;

        String json = ToJsonString.toPrettyJson(sourceJson);

        assertAll(
            () -> assertTrue(json.contains("propOne")),
            () -> assertTrue(json.contains("propTwo"))
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

    @Test
    void toClassInstance_readsSnakeCase() {
        String json = """
            {
                "first_name":"Sarah",
                "surname":"Blackwell",
                "number_of_failed_login_attempts":2
            }
            """;

        TestDto object = ToJsonString.toClassInstance(json, TestDto.class);

        assertNotNull(object);
        assertAll(
            () -> assertEquals("Sarah", object.firstName),
            () -> assertEquals("Blackwell", object.surname),
            () -> assertEquals(2, object.numberOfFailedLoginAttempts)
        );
    }

    @Test
    void toClassInstance_invalidJson_throwsRuntimeException() {
        String invalidJson = """
            {
                "first_name":"Sarah",
                "surname":"Blackwell",
                "number_of_failed_login_attempts":2,
                foo-bar
            }
            """;

        assertThrows(JsonRuntimeException.class, () -> ToJsonString.toClassInstance(invalidJson, TestDto.class));
    }

    @Test
    void toJsonNode_shouldProduceNodeWithSnakeCaseProperties() {
        var testDto = new TestDto("Ben", "Jones", 0);

        JsonNode jsonNode = testDto.toJsonNode();

        assertAll(
            () -> assertEquals("Ben", jsonNode.findValue("first_name").asText()),
            () -> assertEquals("Jones", jsonNode.findValue("surname").asText()),
            () -> assertEquals(0, jsonNode.findValue("number_of_failed_login_attempts").asInt())
        );
    }
}

