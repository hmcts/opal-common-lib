package uk.gov.hmcts.opal.common.dto;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.hmcts.opal.common.user.authorisation.exception.JsonRuntimeException;

public interface ToJsonString {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .disable(WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(new JavaTimeModule());

    default String toJsonString() throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(this);
    }


    static String toJsonString(Object original) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(original);
    }

    default String toJson() {
        try {
            return toJsonString();
        } catch (JsonProcessingException e) {
            throw new JsonRuntimeException(e);
        }
    }

    default String toPrettyJsonString() throws JsonProcessingException {
        return toPrettyJsonString(this);
    }

    static String toPrettyJsonString(Object original) throws JsonProcessingException {
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(original);
    }

    default String toPrettyJson() {
        try {
            return toPrettyJsonString();
        } catch (JsonProcessingException e) {
            throw new JsonRuntimeException(e);
        }
    }

    static String toPrettyJson(String json) {
        try {
            return toPrettyJsonString(toJsonNode(json));
        } catch (JsonProcessingException e) {
            throw new JsonRuntimeException(e);
        }
    }

    static String objectToPrettyJson(Object json) {
        try {
            return toPrettyJsonString(json);
        } catch (JsonProcessingException e) {
            throw new JsonRuntimeException(e);
        }
    }

    static String objectToJson(Object json) {
        try {
            return toJsonString(json);
        } catch (JsonProcessingException e) {
            throw new JsonRuntimeException(e);
        }
    }

    default JsonNode toJsonNode() throws JsonProcessingException {
        return toJsonNode(this.toJsonString());
    }

    static JsonNode toJsonNode(String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(json);
    }

    static Optional<JsonNode> toOptionalJsonNode(String json) {
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(OBJECT_MAPPER.readTree(json));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    static <T> T toClassInstance(String json, Class<T> clss) {
        try {
            return OBJECT_MAPPER.readValue(json, clss);
        } catch (JsonProcessingException e) {
            throw new JsonRuntimeException(e);
        }
    }
}
