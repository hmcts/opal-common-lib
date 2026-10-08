package uk.gov.hmcts.opal.common.docmosis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.common.exceptions.standard.ServiceUnavailableException;
import uk.gov.hmcts.opal.common.config.DocmosisConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PDFGenerationServiceTest {

    private static final String API_KEY = "docmosis-api-key";
    private static final byte[] GENERATED_PDF_CONTENT = "generated-pdf".getBytes();
    @Mock
    private DocmosisClient docmosisClient;

    @Mock
    private DocmosisConfig docmosisConfig;

    @InjectMocks
    private PDFGenerationService service;

    @BeforeEach
    void setUp() {

        when(docmosisConfig.getApiKey()).thenReturn(API_KEY);
    }

    @Test
    void generatePdf_derivesOutputNameFromTemplateName() {
        Map<String, Object> payload = Map.of("account_number", "1234567890");
        mockGeneratedPdf();

        byte[] result = service.generatePdf("fine-notice.docx", payload);

        assertArrayEquals(GENERATED_PDF_CONTENT, result);
        DocmosisRequest request = captureGeneratedRequest();
        assertEquals("fine-notice.docx", request.getTemplateName());
        assertEquals("fine-notice.pdf", request.getOutputName());
        assertEquals(API_KEY, request.getAccessKey());
        assertEquals(payload, request.getData());
    }

    @Test
    void generatePdf_preservesTemplateNameWhenNoExtensionCanBeReplaced() {
        Map<String, Object> payload = Map.of("account_number", "1234567890");
        mockGeneratedPdf();

        byte[] result = service.generatePdf("fine-notice", payload);

        assertArrayEquals(GENERATED_PDF_CONTENT, result);
        DocmosisRequest request = captureGeneratedRequest();
        assertEquals("fine-notice", request.getTemplateName());
        assertEquals("fine-notice", request.getOutputName());
        assertEquals(API_KEY, request.getAccessKey());
        assertEquals(payload, request.getData());
    }

    @Test
    void generatePdf_usesExplicitOutputName() {
        Map<String, Object> payload = Map.of("account_number", "1234567890");
        mockGeneratedPdf();

        byte[] result = service.generatePdf("fine-notice.docx", "custom-output.pdf", payload);

        assertArrayEquals(GENERATED_PDF_CONTENT, result);
        DocmosisRequest request = captureGeneratedRequest();
        assertEquals("fine-notice.docx", request.getTemplateName());
        assertEquals("custom-output.pdf", request.getOutputName());
        assertEquals(API_KEY, request.getAccessKey());
        assertEquals(payload, request.getData());
    }

    @Test
    void generatePdf_wrapsDocmosisClientFailureInServiceUnavailableException() {
        Map<String, Object> payload = Map.of("account_number", "1234567890");
        RuntimeException clientException = new RuntimeException("Docmosis connection failed");
        when(docmosisClient.generatePdf(any(DocmosisRequest.class))).thenThrow(clientException);

        ServiceUnavailableException exception = assertThrows(
            ServiceUnavailableException.class,
            () -> service.generatePdf("fine-notice.docx", payload)
        );

        assertEquals("Service is temporarily unavailable. Please try again later.", exception.getTitle());
        assertEquals("Unexpected error occurred while generating PDF from Docmosis", exception.getDetail());
        assertSame(clientException, exception.getCause());
    }

    private void mockGeneratedPdf() {
        when(docmosisClient.generatePdf(any(DocmosisRequest.class))).thenReturn(GENERATED_PDF_CONTENT);
    }

    private DocmosisRequest captureGeneratedRequest() {
        ArgumentCaptor<DocmosisRequest> captor = ArgumentCaptor.forClass(DocmosisRequest.class);
        verify(docmosisClient).generatePdf(captor.capture());
        return captor.getValue();
    }
}
