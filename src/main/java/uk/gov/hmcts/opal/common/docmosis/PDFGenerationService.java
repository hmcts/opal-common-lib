package uk.gov.hmcts.opal.common.docmosis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.common.exceptions.standard.ServiceUnavailableException;
import uk.gov.hmcts.opal.common.config.DocmosisConfig;

@Service
@RequiredArgsConstructor
public class PDFGenerationService {

    private final DocmosisClient docmosisClient;
    private final DocmosisConfig docmosisConfig;


    public <T> byte[] generatePdf(String templateName, T payload) {
        String outputName = templateName.replaceAll("\\.[^.]+$", ".pdf");
        return generatePdf(templateName, outputName, payload);
    }

    public <T> byte[] generatePdf(String templateName, String outputName, T payload) {
        DocmosisRequest docmosisRequest = DocmosisRequest.builder()
            .templateName(templateName)
            .outputName(outputName)
            .accessKey(docmosisConfig.getApiKey())
            .data(payload)
            .build();
        try {
            return docmosisClient.generatePdf(docmosisRequest);
        } catch (Exception e) {
            throw new ServiceUnavailableException(
                "Service is temporarily unavailable. Please try again later.",
                "Unexpected error occurred while generating PDF from Docmosis",
                e);
        }
    }
}
