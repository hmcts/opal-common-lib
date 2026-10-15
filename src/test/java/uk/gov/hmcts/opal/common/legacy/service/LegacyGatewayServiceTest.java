package uk.gov.hmcts.opal.common.legacy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.xml.sax.SAXParseException;
import tools.jackson.core.JacksonException;
import uk.gov.hmcts.opal.common.legacy.client.LegacyGatewayClient;
import uk.gov.hmcts.opal.common.legacy.config.LegacyGatewayProperties;
import uk.gov.hmcts.opal.common.legacy.model.ErrorResponse;
import uk.gov.hmcts.opal.common.legacy.model.HasErrorResponse;

@ExtendWith(MockitoExtension.class)
class LegacyGatewayServiceTest {

    @Mock
    private LegacyGatewayClient gatewayClient;

    @Mock
    private LegacyGatewayProperties properties;

    private LegacyGatewayService gatewayService;

    @BeforeEach
    void setUp() {
        gatewayService = new LegacyGatewayService(properties, gatewayClient);
    }

    @Test
    void postToGateway_returnsStringResponse() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"status\":\"success\"}", HttpStatus.OK);
        mockGatewayPost(responseEntity);

        GatewayService.Response<String> response = gatewayService.postToGateway("testAction", "{}");

        assertEquals(HttpStatus.OK, response.code);
        assertEquals("{\"status\":\"success\"}", response.body);
        assertFalse(response.isError());
    }

    @Test
    void postToGateway_returnsNullBodyWhenResponseIsEmpty() {
        ResponseEntity<String> responseEntity = ResponseEntity.status(HttpStatus.OK).body(null);
        mockGatewayPost(responseEntity);

        GatewayService.Response<String> response = gatewayService.postToGateway("testAction", "{}");

        assertEquals(HttpStatus.OK, response.code);
        assertNull(response.body);
        assertFalse(response.isError());
    }

    @Test
    void extractResponse_preservesNon2xxStringResponse() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST);

        GatewayService.Response<String> response = gatewayService.extractResponse(responseEntity, String.class, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.code);
        assertEquals("Error", response.body);
        assertTrue(response.isError());
    }

    @Test
    void postToGateway_unmarshalsXmlDtoResponse() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>(testEntityXml(), HttpStatus.OK);
        mockGatewayPost(responseEntity);

        GatewayService.Response<TestDto> response =
            gatewayService.postToGateway("testAction", TestDto.class, "{}", null);

        assertNotNull(response.responseEntity);
        assertEquals(HttpStatus.OK, response.code);
        assertEquals(1L, response.responseEntity.getTestId());
        assertEquals("NT", response.responseEntity.getTestType());
        assertFalse(response.isError());
    }

    @Test
    void postToGateway_invalidXmlReturnsExceptionResponse() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"test_id\":\"bad\"}", HttpStatus.OK);
        mockGatewayPost(responseEntity);

        GatewayService.Response<TestDto> response =
            gatewayService.postToGateway("testAction", TestDto.class, "{}", null);

        assertTrue(response.isError());
        assertInstanceOf(UnmarshalException.class, response.exception);
        assertInstanceOf(SAXParseException.class, response.exception.getCause());
        assertEquals(HttpStatus.OK, response.code);
        assertEquals("{\"test_id\":\"bad\"}", response.body);
    }

    @Test
    void postToGateway_invalidEntityReturnsExceptionResponse() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>(brokenEntityXml(), HttpStatus.OK);
        mockGatewayPost(responseEntity);

        GatewayService.Response<TestDto> response =
            gatewayService.postToGateway("testAction", TestDto.class, "{}", null);

        assertTrue(response.isError());
        assertInstanceOf(UnmarshalException.class, response.exception);
        assertTrue(response.exception.getMessage().contains("Expected elements are <{}testEntity>"));
        assertEquals(HttpStatus.OK, response.code);
    }

    @Test
    void postToGatewayAsync_returnsCompletedFuture() throws Exception {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"status\":\"success\"}", HttpStatus.OK);
        mockGatewayPost(responseEntity);

        CompletableFuture<GatewayService.Response<String>> future =
            gatewayService.postToGatewayAsync("testAction", String.class, "{}", null);

        assertTrue(future.isDone());
        assertEquals("{\"status\":\"success\"}", future.get().body);
    }

    @Test
    void postParamsToGateway_wrapsJsonProcessingFailure() {
        RuntimeException thrown = assertThrows(
            RuntimeException.class,
            () -> gatewayService.postParamsToGateway("testAction", TestDto.class, new BrokenMapImplementation<>())
        );

        assertInstanceOf(JacksonException.class, thrown.getCause());
    }

    @Test
    void extractResponse_returnsInternalServerErrorWhenResponseEntityIsNull() {
        GatewayService.Response<TestDto> response = gatewayService.extractResponse(null, TestDto.class, null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.code);
        assertNull(response.responseEntity);
    }

    @Test
    void responseTreatsEmbeddedLegacyErrorsAsErrors() {
        GatewayService.Response<LegacyErrorEnvelope> response = new GatewayService.Response<>(
            HttpStatus.OK,
            new LegacyErrorEnvelope(new ErrorResponse("123", "boom"))
        );

        assertTrue(response.hasErrorResponse());
        assertTrue(response.isError());
    }

    @Test
    void postToGateway_detectsEmbeddedLegacyError() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("""
            <response><error_response><error_code>123</error_code><error_message>boom</error_message>
            </error_response></response>
            """, HttpStatus.OK);
        mockGatewayPost(responseEntity);

        GatewayService.Response<LegacyErrorEnvelope> response = gatewayService.postToGateway(
            "testAction", LegacyErrorEnvelope.class, "{}", null);

        assertTrue(response.hasErrorResponse());
        assertEquals("123", response.responseEntity.getErrorResponse().getErrorCode());
    }

    private void mockGatewayPost(ResponseEntity<String> responseEntity) {
        when(properties.getUrl()).thenReturn("http://test.com");
        when(properties.getUsername()).thenReturn("user");
        when(properties.getPassword()).thenReturn("password");
        when(gatewayClient.post(anyString(), anyString(), any())).thenReturn(responseEntity);
    }

    private String testEntityXml() {
        return """
            <testEntity>
              <testId>1</testId>
              <testType>NT</testType>
            </testEntity>
            """;
    }

    private String brokenEntityXml() {
        return """
            <wrongEntity>
              <testId>1</testId>
              <testType>NT</testType>
            </wrongEntity>
            """;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement(name = "testEntity")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class TestDto {

        @XmlElement(name = "testId")
        private Long testId;

        @XmlElement(name = "testType")
        private String testType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement(name = "response")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class LegacyErrorEnvelope implements HasErrorResponse {

        @XmlElement(name = "error_response")
        private ErrorResponse errorResponse;
    }

    static class BrokenMapImplementation<K, V> implements Map<K, V> {

        @Override
        public int size() {
            return 9;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public V get(Object key) {
            return null;
        }

        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public java.util.Set<K> keySet() {
            return null;
        }

        @Override
        public java.util.Collection<V> values() {
            return null;
        }

        @Override
        public java.util.Set<Entry<K, V>> entrySet() {
            return null;
        }
    }
}
