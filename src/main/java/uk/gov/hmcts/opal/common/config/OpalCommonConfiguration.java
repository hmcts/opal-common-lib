package uk.gov.hmcts.opal.common.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@Validated
@ConfigurationProperties(prefix = "opal.common")
public class OpalCommonConfiguration {

    @NotNull
    @NotBlank
    private String domain;

    @NotNull
    private SystemUsers systemUsers;

    @Data
    public static class SystemUsers {

        @NotNull
        @NotBlank
        private String tokenUrl;

        private Map<String, SystemUser> users;

    }

    @Data
    public static class SystemUser {

        @NotBlank
        private String clientId;

        @NotBlank
        private String clientSecret;

        @NotBlank
        private String scope;

        @NotBlank
        private String grantType;
    }
}
