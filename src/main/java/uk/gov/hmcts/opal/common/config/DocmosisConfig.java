package uk.gov.hmcts.opal.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@Validated
@ConfigurationProperties(prefix = "opal.common.docmosis")
public class DocmosisConfig {

    private String url;
    private String apiKey;
}