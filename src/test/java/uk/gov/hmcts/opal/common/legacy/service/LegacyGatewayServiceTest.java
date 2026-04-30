package uk.gov.hmcts.opal.common.legacy.service;

import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.xml.sax.SAXParseException;
import uk.gov.hmcts.opal.common.legacy.config.LegacyGatewayProperties;
import uk.gov.hmcts.opal.common.legacy.model.ErrorResponse;
import uk.gov.hmcts.opal.common.legacy.model.HasErrorResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

@ExtendWith(MockitoExtension.class)
class LegacyGatewayServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private LegacyGatewayProperties properties;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private LegacyGatewayService gatewayService;

    @Test
    void postToGateway_returnsStringResponse() {
        mockRestClientPost();
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"status\":\"success\"}", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        GatewayService.Response<String> response = gatewayService.postToGateway("testAction", "{}");

        assertEquals(HttpStatus.OK, response.code);
        assertEquals("{\"status\":\"success\"}", response.body);
        assertFalse(response.isError());
    }

    @Test
    void postToGateway_returnsNullBodyWhenResponseIsEmpty() {
        mockRestClientPost();
        ResponseEntity<String> responseEntity = ResponseEntity.status(HttpStatus.OK).body(null);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        GatewayService.Response<String> response = gatewayService.postToGateway("testAction", "{}");

        assertEquals(HttpStatus.OK, response.code);
        assertNull(response.body);
        assertFalse(response.isError());
    }

    @Test
    void postToGateway_returnsNon2xxStringResponse() {
        mockRestClientPost();
        ResponseEntity<String> responseEntity = new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        GatewayService.Response<String> response = gatewayService.postToGateway("testAction", "{}");

        assertEquals(HttpStatus.BAD_REQUEST, response.code);
        assertEquals("Error", response.body);
        assertTrue(response.isError());
    }

    @Test
    void postToGateway_unmarshalsXmlDtoResponse() {
        mockRestClientPost();
        ResponseEntity<String> responseEntity = new ResponseEntity<>(testEntityXml(), HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

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
        mockRestClientPost();
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"test_id\":\"bad\"}", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

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
        mockRestClientPost();
        ResponseEntity<String> responseEntity = new ResponseEntity<>(brokenEntityXml(), HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        GatewayService.Response<TestDto> response =
            gatewayService.postToGateway("testAction", TestDto.class, "{}", null);

        assertTrue(response.isError());
        assertInstanceOf(UnmarshalException.class, response.exception);
        assertTrue(response.exception.getMessage().contains("Expected elements are <{}testEntity>"));
        assertEquals(HttpStatus.OK, response.code);
    }

    @Test
    void postToGatewayAsync_returnsCompletedFuture() throws Exception {
        mockRestClientPost();
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"status\":\"success\"}", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

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

        assertTrue(thrown.getMessage().contains("JsonMappingException"));
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

    private void mockRestClientPost() {
        when(properties.getUrl()).thenReturn("http://test.com");
        when(properties.getUsername()).thenReturn("user");
        when(properties.getPassword()).thenReturn("password");
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
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
    @AllArgsConstructor
    static class LegacyErrorEnvelope implements HasErrorResponse {

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
