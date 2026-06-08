package uk.gov.hmcts.opal.common.dto;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.opal.common.user.authorisation.exception.JsonRuntimeException;

import java.util.Optional;

import static tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;

public interface ToJsonString {

    ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .disable(WRITE_DATES_AS_TIMESTAMPS)
        .build();

    default String toJsonString() throws JacksonException {
        return OBJECT_MAPPER.writeValueAsString(this);
    }


    static String toJsonString(Object original) throws JacksonException {
        return OBJECT_MAPPER.writeValueAsString(original);
    }

    default String toJson() {
        try {
            return toJsonString();
        } catch (JacksonException e) {
            throw new JsonRuntimeException(e);
        }
    }

    default String toPrettyJsonString() throws JacksonException {
        return toPrettyJsonString(this);
    }

    static String toPrettyJsonString(Object original) throws JacksonException {
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(original);
    }

    default String toPrettyJson() {
        try {
            return toPrettyJsonString();
        } catch (JacksonException e) {
            throw new JsonRuntimeException(e);
        }
    }

    static String toPrettyJson(String json) {
        try {
            return toPrettyJsonString(toJsonNode(json));
        } catch (JacksonException e) {
            throw new JsonRuntimeException(e);
        }
    }

    static String objectToPrettyJson(Object json) {
        try {
            return toPrettyJsonString(json);
        } catch (JacksonException e) {
            throw new JsonRuntimeException(e);
        }
    }

    static String objectToJson(Object json) {
        try {
            return toJsonString(json);
        } catch (JacksonException e) {
            throw new JsonRuntimeException(e);
        }
    }

    default JsonNode toJsonNode() throws JacksonException {
        return toJsonNode(this.toJsonString());
    }

    static JsonNode toJsonNode(String json) throws JacksonException {
        return OBJECT_MAPPER.readTree(json);
    }

    static Optional<JsonNode> toOptionalJsonNode(String json) {
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(OBJECT_MAPPER.readTree(json));
        } catch (JacksonException e) {
            return Optional.empty();
        }
    }

    static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    static <T> T toClassInstance(String json, Class<T> clss) {
        try {
            return OBJECT_MAPPER.readValue(json, clss);
        } catch (JacksonException e) {
            throw new JsonRuntimeException(e);
        }
    }
}
