package uk.gov.hmcts.opal.common.legacy.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;

import java.util.Map;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.opal.common.legacy.config.LegacyGatewayProperties;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.opal.common.legacy.service.LegacyGatewayService;

@SpringBootTest(classes = LegacyGatewayClientIntegrationTest.TestApplication.class)
@TestConstructor(autowireMode = ALL)
class LegacyGatewayClientIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private final GatewayService gatewayService;

    LegacyGatewayClientIntegrationTest(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("legacy-gateway.url", () -> wireMockServer.baseUrl() + "/opal");
        registry.add("legacy-gateway.feign-enabled", () -> "true");
        registry.add("legacy-gateway.username", () -> "user");
        registry.add("legacy-gateway.password", () -> "password");
    }

    @Test
    void postToGateway_sendsLegacyRequest() {
        wireMockServer.stubFor(post(urlPathEqualTo("/opal"))
            .willReturn(aResponse().withStatus(200).withBody("response")));

        GatewayService.Response<String> response = gatewayService.postToGateway(
            "getAccount", String.class, "{}", null);

        assertThat(response.body).isEqualTo("response");
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/opal"))
            .withQueryParam("actionType", equalTo("getAccount"))
            .withHeader("Authorization", equalTo("Basic dXNlcjpwYXNzd29yZA=="))
            .withHeader("Content-Type", matching("application/json.*"))
            .withHeader("Accept", matching("application/xml.*"))
            .withRequestBody(equalTo("{}")));
    }

    @Test
    void postToGateway_serializesObjectRequestAsJson() {
        wireMockServer.stubFor(post(urlPathEqualTo("/opal"))
            .willReturn(aResponse().withStatus(200).withBody("response")));

        gatewayService.postToGateway("getAccount", String.class, Map.of("account_id", 77), null);

        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/opal"))
            .withRequestBody(equalTo("{\"account_id\":77}")));
    }

    @Test
    void postToGateway_preservesEmptyResponseStatus() {
        wireMockServer.stubFor(post(urlPathEqualTo("/opal"))
            .willReturn(aResponse().withStatus(204)));

        GatewayService.Response<String> response = gatewayService.postToGateway(
            "getAccount", String.class, "{}", null);

        assertThat(response.code).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.body).isNull();
    }

    @Test
    void postToGateway_preservesNotFoundExceptionType() {
        wireMockServer.stubFor(post(urlPathEqualTo("/opal"))
            .willReturn(aResponse().withStatus(404).withBody("Not found")));

        assertThatExceptionOfType(HttpClientErrorException.NotFound.class)
            .isThrownBy(() -> gatewayService.postToGateway("getAccount", String.class, "{}", null))
            .satisfies(error -> assertThat(error.getResponseBodyAsString()).isEqualTo("Not found"));
    }

    @Test
    void postToGateway_preservesServerErrorExceptionType() {
        wireMockServer.stubFor(post(urlPathEqualTo("/opal"))
            .willReturn(aResponse().withStatus(500).withBody("Legacy failed")));

        assertThatThrownBy(() -> gatewayService.postToGateway("getAccount", String.class, "{}", null))
            .isInstanceOf(HttpServerErrorException.InternalServerError.class)
            .hasMessageContaining("500");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableFeignClients(basePackages = "uk.gov.hmcts.opal.common.legacy")
    @EnableConfigurationProperties(LegacyGatewayProperties.class)
    @Import(LegacyGatewayClientConfiguration.class)
    static class TestApplication {

        @Bean
        GatewayService gatewayService(LegacyGatewayProperties properties, LegacyGatewayClient client) {
            return new LegacyGatewayService(properties, client);
        }
    }
}
