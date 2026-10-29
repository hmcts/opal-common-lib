package uk.gov.hmcts.opal.common.docmosis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import uk.gov.hmcts.opal.common.config.DocmosisConfig;

@SpringBootTest(classes = DocmosisDisabledIntegrationTest.TestApplication.class)
@TestConstructor(autowireMode = ALL)
class DocmosisDisabledIntegrationTest {

    private final ApplicationContext context;

    DocmosisDisabledIntegrationTest(ApplicationContext context) {
        this.context = context;
    }

    @Test
    void doesNotRegisterDocmosisWithoutConfiguration() {
        assertThat(context.getBeansOfType(DocmosisClient.class)).isEmpty();
        assertThat(context.getBeansOfType(PDFGenerationService.class)).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(DocmosisConfig.class)
    @EnableFeignClients(basePackages = "uk.gov.hmcts.opal.common.docmosis")
    @Import({DocmosisClientConfiguration.class, PDFGenerationService.class})
    static class TestApplication {
    }
}
