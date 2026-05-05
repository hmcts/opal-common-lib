package uk.gov.hmcts.opal.common.legacy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.opal.common.dto.ToJsonString;
import uk.gov.hmcts.opal.common.legacy.config.LegacyGatewayProperties;
import uk.gov.hmcts.opal.common.xml.XmlUtil;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j(topic = "opal.LegacyGatewayService")
@RequiredArgsConstructor
public class LegacyGatewayService implements GatewayService {

    public static final String ACTION_TYPE = "actionType";

    protected final LegacyGatewayProperties gatewayProperties;
    protected final RestClient restClient;

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

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("")
            .queryParam(ACTION_TYPE, actionType);

        RestClient.RequestBodySpec bodySpec = restClient.post()
            .uri(gatewayProperties.getUrl() + builder.toUriString())
            .header("AUTHORIZATION", encodeBasic(gatewayProperties.getUsername(), gatewayProperties.getPassword()))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_XML)
            .body(request);

        ResponseEntity<String> responseEntity = bodySpec.retrieve().toEntity(String.class);
        return extractResponse(responseEntity, responseType, responseSchemaFile);
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
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String encodeBasic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
