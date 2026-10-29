package uk.gov.hmcts.opal.common.legacy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.opal.common.legacy.config.LegacyGatewayProperties;

class LegacyGatewayRestClientTest {

    @Test
    void existingConstructorUsesRestClient() {
        LegacyGatewayProperties properties = new LegacyGatewayProperties();
        properties.setUrl("http://legacy.test/opal");
        properties.setUsername("user");
        properties.setPassword("password");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://legacy.test/opal?actionType=testAction"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNzd29yZA=="))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE))
            .andRespond(withSuccess("<response/>", MediaType.APPLICATION_XML));

        LegacyGatewayService service = new LegacyGatewayService(properties, builder.build());
        GatewayService.Response<String> response = service.postToGateway("testAction", "{}");

        assertEquals(HttpStatus.OK, response.code);
        assertEquals("<response/>", response.body);
        server.verify();
    }
}
