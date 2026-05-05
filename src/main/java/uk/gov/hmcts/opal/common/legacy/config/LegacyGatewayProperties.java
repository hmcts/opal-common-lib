package uk.gov.hmcts.opal.common.legacy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "legacy-gateway")
public class LegacyGatewayProperties {

    private String url;
    private String username;
    private String password;
}
