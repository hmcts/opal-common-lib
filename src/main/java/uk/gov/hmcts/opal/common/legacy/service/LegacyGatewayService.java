package uk.gov.hmcts.opal.common.legacy.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import uk.gov.hmcts.opal.common.dto.ToJsonString;
import uk.gov.hmcts.opal.common.legacy.client.LegacyGatewayClient;
import uk.gov.hmcts.opal.common.legacy.config.LegacyGatewayProperties;
import uk.gov.hmcts.opal.common.xml.XmlUtil;

/**
 * Supports the existing RestClient path while consumers migrate explicitly to Feign.
 * Consumers using Feign must enable {@code legacy-gateway.feign-enabled} to register the client.
 */
@Slf4j(topic = "opal.LegacyGatewayService")
public class LegacyGatewayService implements GatewayService {

    public static final String ACTION_TYPE = "actionType";

    protected final LegacyGatewayProperties gatewayProperties;
    private final GatewayTransport transport;

    public LegacyGatewayService(LegacyGatewayProperties gatewayProperties, RestClient restClient) {
        this.gatewayProperties = gatewayProperties;
        this.transport = (actionType, authorization, request) -> {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("")
                .queryParam(ACTION_TYPE, actionType);

            return restClient.post()
                .uri(gatewayProperties.getUrl() + builder.toUriString())
                .header("AUTHORIZATION", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_XML)
                .body(request)
                .retrieve().toEntity(String.class);
        };
    }

    public LegacyGatewayService(LegacyGatewayProperties gatewayProperties, LegacyGatewayClient gatewayClient) {
        this.gatewayProperties = gatewayProperties;
        this.transport = (actionType, authorization, request) -> {
            try {
                return gatewayClient.post(actionType, authorization, request);
            } catch (FeignException exception) {
                throw toRestClientException(exception);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public <T> Response<T> extractResponse(ResponseEntity<String> responseEntity, Class<T> clzz, String schemaFile) {
        if (responseEntity == null) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR, (T) null);
        }

        HttpStatusCode code = responseEntity.getStatusCode();

        if (clzz.equals(String.class)) {
            return new Response<>(code, responseEntity.getBody());
        }

        if (!StringUtils.hasText(responseEntity.getBody())) {
            log.warn("extractResponse: Received an empty body in the response from the Legacy Gateway.");
            return new Response<>(code, (T) null);
        }

        String rawXml = responseEntity.getBody();
        log.info("extractResponse: Raw XML response: \n{}", rawXml);

        XmlUtil.validateXmlString(schemaFile, rawXml);

        try {
            T entity = XmlUtil.unmarshalXmlString(rawXml, clzz);
            return new Response<>(code, entity);
        } catch (Exception e) {
            log.error("extractResponse: Error deserializing response: {}", e.getMessage(), e);
            return new Response<>(code, e, rawXml);
        }
    }

    @Override
    public <T> Response<T> postToGateway(
        String actionType, Class<T> responseType, Object request, String responseSchemaFile) {

        log.info("postToGateway: POST to Gateway: {}?{}={}", gatewayProperties.getUrl(), ACTION_TYPE, actionType);

        ResponseEntity<String> response = transport.post(actionType,
            encodeBasic(gatewayProperties.getUsername(), gatewayProperties.getPassword()), request);
        return extractResponse(response, responseType, responseSchemaFile);
    }

    public Response<String> postToGateway(String actionType, Object request) {
        return postToGateway(actionType, String.class, request, null);
    }

    @Override
    @Async
    public <T> CompletableFuture<Response<T>> postToGatewayAsync(
        String actionType, Class<T> responseType, Object request, String responseSchemaFile) {

        log.debug(
            "postToGatewayAsync: ASYNC POST to Gateway: {}?{}={}",
            gatewayProperties.getUrl(), ACTION_TYPE, actionType);

        long start = System.currentTimeMillis();
        Response<T> response = postToGateway(actionType, responseType, request, responseSchemaFile);
        long end = System.currentTimeMillis();

        log.debug("postToGatewayAsync: ASYNC POST to Gateway response in {} seconds.", (end - start) / 1000f);
        return CompletableFuture.completedFuture(response);
    }

    public <T> Response<T> postParamsToGateway(
        String actionType, Class<T> responseType, Map<String, Object> requestParams) {
        try {
            return postToGateway(
                actionType,
                responseType,
                ToJsonString.getObjectMapper().writeValueAsString(requestParams),
                null
            );
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    private String encodeBasic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password)
            .getBytes(StandardCharsets.UTF_8));
    }

    private RuntimeException toRestClientException(FeignException exception) {
        int rawStatus = exception.status();
        if (rawStatus < 400 || rawStatus >= 600) {
            return exception;
        }

        HttpStatusCode status = HttpStatusCode.valueOf(rawStatus);
        HttpStatus knownStatus = HttpStatus.resolve(rawStatus);
        String statusText = knownStatus != null ? knownStatus.getReasonPhrase() : "";
        HttpHeaders headers = new HttpHeaders();
        exception.responseHeaders().forEach((name, values) -> values.forEach(value -> headers.add(name, value)));
        byte[] body = exception.responseBody().map(buffer -> {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return bytes;
        }).orElseGet(() -> new byte[0]);

        if (status.is4xxClientError()) {
            return HttpClientErrorException.create(status, statusText, headers,
                body, StandardCharsets.UTF_8);
        }
        return HttpServerErrorException.create(status, statusText, headers,
            body, StandardCharsets.UTF_8);
    }

    /**
     * Keeps the existing RestClient path and opt-in Feign path behind the same request contract during migration.
     * Remove this bridge once all consumers use Feign.
     */
    @FunctionalInterface
    private interface GatewayTransport {

        ResponseEntity<String> post(String actionType, String authorization, Object request);
    }
}
