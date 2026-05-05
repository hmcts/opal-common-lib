package uk.gov.hmcts.opal.common.launchdarkly.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "launchdarkly")
public class LaunchDarklyProperties {

    /**
     * true to enable launchdarkly.
     */
    private Boolean enabled;

    /**
     * sdk key to connect to launchdarkly.
     */
    private String sdkKey;

    /**
     * true to use launchdarkly offline mode.
     */
    private Boolean offlineMode;

    /**
     * (optional) a list of paths to json or yaml files containing flags for launchdarkly.
     * If there are duplicate keys, the first files have precedence.
     */
    private String[] file;

    private String env;

    private Map<String, Boolean> defaultFlagValues = new HashMap<>();

    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public boolean getDefaultFlagValue(String feature, boolean fallback) {
        return defaultFlagValues == null ? fallback : defaultFlagValues.getOrDefault(feature, fallback);
    }
}
