package uk.gov.hmcts.opal.common.docmosis;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "docmosisClient",
    url = "${opal.common.docmosis.url}"
)
public interface DocmosisClient {

    @PostMapping("/api/render")
    byte[] generatePdf(DocmosisRequest docmosisRequest);

}
