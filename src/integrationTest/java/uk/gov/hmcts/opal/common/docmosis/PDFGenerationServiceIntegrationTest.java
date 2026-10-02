package uk.gov.hmcts.opal.common.docmosis;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.opal.common.config.DocmosisConfig;

@SpringBootTest(classes = PDFGenerationServiceIntegrationTest.TestApplication.class)
@ActiveProfiles("integration")
class PDFGenerationServiceIntegrationTest {

    private static final String API_KEY = "docmosis-api-key";

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @Autowired
    private PDFGenerationService pdfGenerationService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("opal.common.docmosis.url", wireMockServer::baseUrl);
        registry.add("opal.common.docmosis.api-key", () -> API_KEY);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(DocmosisConfig.class)
    @EnableFeignClients(clients = DocmosisClient.class)
    @Import(PDFGenerationService.class)
    static class TestApplication {

    }

    @Test
    void generatePdf_postsDocmosisRequestAndReturnsGeneratedPdf() {
        byte[] generatedPdf = "generated-pdf".getBytes();
        wireMockServer.stubFor(post(urlEqualTo("/api/render"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/pdf")
                .withBody(generatedPdf)));

        byte[] result = pdfGenerationService.generatePdf(
            "fine-notice.docx",
            Map.of("account_number", "1234567890")
        );

        assertThat(result).isEqualTo(generatedPdf);
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/render"))
            .withRequestBody(equalToJson("""
                {
                  "templateName": "fine-notice.docx",
                  "outputName": "fine-notice.pdf",
                  "accessKey": "docmosis-api-key",
                  "data": {
                    "account_number": "1234567890"
                  }
                }
                """)));
    }

    @Test
    void generatePdf_postsExplicitOutputName() {
        byte[] generatedPdf = "custom-generated-pdf".getBytes();
        wireMockServer.stubFor(post(urlEqualTo("/api/render"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/pdf")
                .withBody(generatedPdf)));

        byte[] result = pdfGenerationService.generatePdf(
            "fine-notice.docx",
            "custom-output.pdf",
            Map.of("account_number", "1234567890")
        );

        assertThat(result).isEqualTo(generatedPdf);
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/render"))
            .withRequestBody(equalToJson("""
                {
                  "templateName": "fine-notice.docx",
                  "outputName": "custom-output.pdf",
                  "accessKey": "docmosis-api-key",
                  "data": {
                    "account_number": "1234567890"
                  }
                }
                """)));
    }

    @Test
    void generatePdf_mapsDtoPayloadIntoDocmosisData() {
        byte[] generatedPdf = "dto-generated-pdf".getBytes();
        wireMockServer.stubFor(post(urlEqualTo("/api/render"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/pdf")
                .withBody(generatedPdf)));

        byte[] result = pdfGenerationService.generatePdf(
            "fine-notice.docx",
            new FineNoticeData("1234567890", "Jane Smith", 12500)
        );

        assertThat(result).isEqualTo(generatedPdf);
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/render"))
            .withRequestBody(equalToJson("""
                {
                  "templateName": "fine-notice.docx",
                  "outputName": "fine-notice.pdf",
                  "accessKey": "docmosis-api-key",
                  "data": {
                    "accountNumber": "1234567890",
                    "defendantName": "Jane Smith",
                    "amountInPence": 12500
                  }
                }
                """)));
    }

    record FineNoticeData(String accountNumber, String defendantName, int amountInPence) {

    }
}
