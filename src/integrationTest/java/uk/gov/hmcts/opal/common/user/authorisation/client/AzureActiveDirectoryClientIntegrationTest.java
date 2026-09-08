package uk.gov.hmcts.opal.common.user.authorisation.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.AzureToken;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AzureActiveDirectoryClientIntegrationTest.TestApplication.class)
@ActiveProfiles("integration")
class AzureActiveDirectoryClientIntegrationTest {

    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final String SCOPE = "scope-value";
    private static final String GRANT_TYPE = "client_credentials";

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @Autowired
    private AzureActiveDirectoryClient azureActiveDirectoryClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("opal.common.system-users.token-url", wireMockServer::baseUrl);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableFeignClients(clients = AzureActiveDirectoryClient.class)
    static class TestApplication {

    }

    @Test
    void getSystemUser_sendsFormUrlEncodedBodyUsingPost() {
        wireMockServer.stubFor(post(urlEqualTo("/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "token_type": "Bearer",
                      "expires_in": 3600,
                      "ext_expires_in": 3600,
                      "access_token": "token-value"
                    }
                    """)));

        AzureActiveDirectoryClient.GetSystemUserFormData formData = new AzureActiveDirectoryClient.GetSystemUserFormData(CLIENT_ID, CLIENT_SECRET, SCOPE, GRANT_TYPE);

        AzureToken token = azureActiveDirectoryClient.getSystemUser(formData);

        assertThat(token.getAccessToken()).isEqualTo("token-value");

        wireMockServer
            .verify(postRequestedFor(urlPathEqualTo("/"))
                .withHeader("Content-Type",
                    matching("application/x-www-form-urlencoded(;\\s*charset=UTF-8)?"))
                .withRequestBody(equalTo(
                    "client_id=client-id&client_secret=client-secret&scope=scope-value&grant_type=client_credentials"
                )));
    }
}




