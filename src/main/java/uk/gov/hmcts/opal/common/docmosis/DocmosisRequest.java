package uk.gov.hmcts.opal.common.docmosis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocmosisRequest {

    @JsonProperty("templateName")
    private String templateName;
    @JsonProperty("outputName")
    private String outputName;
    @JsonProperty("accessKey")
    private String accessKey;
    @JsonProperty("data")
    private Object data;

}