package uk.gov.hmcts.opal.common.docmosis;

import org.springframework.web.bind.annotation.PostMapping;

public interface DocmosisClient {

    @PostMapping("/api/render")
    byte[] generatePdf(DocmosisRequest docmosisRequest);

}
