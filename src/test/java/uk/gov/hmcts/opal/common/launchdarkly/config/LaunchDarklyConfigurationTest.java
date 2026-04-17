package uk.gov.hmcts.opal.common.launchdarkly.config;

import com.launchdarkly.sdk.server.LDClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

class LaunchDarklyConfigurationTest {

    private final LaunchDarklyProperties properties = new LaunchDarklyProperties();

    private final LaunchDarklyConfiguration configuration = new LaunchDarklyConfiguration();

    /**
     * LDClient build test without flag files.
     */
    @Test
    void streamingLd() {
        String key = "sdkkey";
        Boolean offline = false;
        String[] file = new String[0];
        properties.setSdkKey(key);
        properties.setOfflineMode(offline);
        properties.setFile(file);

        LDClient client = configuration.ldClient(properties);
        Assertions.assertEquals(client.isOffline(), properties.getOfflineMode());

        client = configuration.ldClient(properties);
        Assertions.assertEquals(client.isOffline(), offline);
    }

    @Test
    void nonExistentFiles() {
        String key = "sdkkey";
        Boolean offline = false;
        String[] file = new String[]{
            "AFileThatDoesNotExist"
        };
        properties.setSdkKey(key);
        properties.setOfflineMode(offline);
        properties.setFile(file);
        LDClient client = configuration.ldClient(properties);
        Assertions.assertEquals(client.isOffline(), offline);
    }

    @Test
    void withFlags() {
        String path = "./bin/utils/launchdarkly-flags.json";
        if (Files.exists(Paths.get(path))) {
            String key = "sdkkey";
            Boolean offline = false;
            properties.setSdkKey(key);
            properties.setOfflineMode(offline);
            properties.setFile(new String[]{path});

            LDClient client = configuration.ldClient(properties);

            Assertions.assertEquals(client.isOffline(), offline);
        }
    }
}
