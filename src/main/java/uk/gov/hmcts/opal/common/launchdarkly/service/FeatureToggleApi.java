package uk.gov.hmcts.opal.common.launchdarkly.service;

import com.launchdarkly.sdk.ContextBuilder;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.launchdarkly.config.LaunchDarklyProperties;

import java.io.IOException;
import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureToggleApi {

    private final LDClientInterface internalClient;
    private final LaunchDarklyProperties properties;
    private final Environment environment;
    private final Clock clock;

    @PostConstruct
    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public boolean isFeatureEnabled(String feature) {
        return isFeatureEnabled(feature, properties.getDefaultFlagValue(feature, false));
    }

    public boolean isFeatureEnabled(String feature, boolean defaultValue) {
        if (!properties.isEnabled()) {
            return defaultValue;
        }
        return internalClient.boolVariation(feature, createLdContext().build(), defaultValue);
    }

    public boolean isFeatureEnabled(String feature, LDContext context) {
        return isFeatureEnabled(feature, context, properties.getDefaultFlagValue(feature, false));
    }

    public boolean isFeatureEnabled(String feature, LDContext context, boolean defaultValue) {
        if (!properties.isEnabled()) {
            return defaultValue;
        }
        return internalClient.boolVariation(feature, context, defaultValue);
    }

    public String getFeatureValue(String feature, String defaultValue) {
        if (!properties.isEnabled()) {
            return defaultValue;
        }
        return internalClient.stringVariation(feature, createLdContext().build(), defaultValue);
    }

    public ContextBuilder createLdContext() {
        return LDContext.builder(this.properties.getSdkKey())
            .set("timestamp", String.valueOf(clock.millis()))
            .set("environment", properties.getEnv());
    }

    private void close() {
        try {
            internalClient.close();
        } catch (IOException e) {
            log.error("Error in closing the Launchdarkly client::", e);
        }
    }


    public boolean isFeatureEnabledWithPropertyValueDefault(String feature, String defaultValueProperty) {
        return isFeatureEnabledWithPropertyValueDefault(feature, defaultValueProperty, false);
    }

    public boolean isFeatureEnabledWithPropertyValueDefault(String feature, String defaultValueProperty,
                                                            boolean fallBack) {
        boolean defaultValue = resolveDefaultValue(defaultValueProperty, fallBack);
        if (!properties.isEnabled()) {
            log.debug("Launch darkly is disabled: using default value fallback {} for feature {}",
                defaultValue, feature);
            return defaultValue;
        }
        return isFeatureEnabled(feature, defaultValue);
    }


    private boolean resolveDefaultValue(String defaultValueProperty, boolean fallBack) {
        if (defaultValueProperty == null || defaultValueProperty.isBlank()) {
            return fallBack;
        }

        String resolvedValue = environment.resolvePlaceholders(defaultValueProperty);
        if (!resolvedValue.equals(defaultValueProperty)) {
            return Boolean.parseBoolean(resolvedValue);
        }

        return Boolean.parseBoolean(environment.getProperty(defaultValueProperty, String.valueOf(fallBack)));
    }
}
