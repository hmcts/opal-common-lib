package uk.gov.hmcts.opal.common.contentdigest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import uk.gov.hmcts.opal.common.controllers.advice.OpalGlobalExceptionHandler;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContentDigestIntegrationTest {

    private static final String ENDPOINT = "/content-digest-test";
    private static final String REQUEST_BODY = "request-body";
    private static final String RESPONSE_BODY = "response-body";

    @Test
    void defaultProperties_allowMissingHeaderAndDoNotAddResponseHeader() throws Exception {
        try (TestMvcContext context = defaultTestMvc()) {
            context.mockMvc().perform(post(ENDPOINT)
                                          .contentType(MediaType.TEXT_PLAIN)
                                          .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(ContentDigestValidatorInterceptor.CONTENT_DIGEST));
        }
    }

    @Test
    void contentDigestDisabled_allowsMissingHeaderAndDoesNotAddResponseHeader() throws Exception {
        try (TestMvcContext context = testMvc(false, false)) {
            context.mockMvc().perform(post(ENDPOINT)
                                          .contentType(MediaType.TEXT_PLAIN)
                                          .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(ContentDigestValidatorInterceptor.CONTENT_DIGEST));
        }
    }

    @Test
    void responseAutoGenerationEnabled_addsValidResponseContentDigest() throws Exception {
        try (TestMvcContext context = testMvc(true, false)) {
            MvcResult result = context.mockMvc().perform(post(ENDPOINT)
                                                             .contentType(MediaType.TEXT_PLAIN)
                                                             .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andReturn();

            assertThat(result.getResponse().getHeader(ContentDigestValidatorInterceptor.CONTENT_DIGEST))
                .isEqualTo(contentDigestHeaderFor("sha-512", "SHA-512",
                                                  result.getResponse().getContentAsByteArray()));
        }
    }

    @Test
    void optionalRequestValidation_rejectsInvalidProvidedContentDigest() throws Exception {
        try (TestMvcContext context = testMvc(true, false)) {
            context.mockMvc().perform(post(ENDPOINT)
                                          .contentType(MediaType.TEXT_PLAIN)
                                          .header(ContentDigestValidatorInterceptor.CONTENT_DIGEST,
                                                  contentDigestHeaderFor("sha-512", "SHA-512", "different-body"))
                                          .content(REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Digest validation failed"))
                .andExpect(jsonPath("$.detail").value("Body hash did not match for algorithm: sha-512"));
        }
    }

    @Test
    void enforcedRequestValidation_rejectsInvalidContentDigest() throws Exception {
        try (TestMvcContext context = testMvc(true, true)) {
            context.mockMvc().perform(post(ENDPOINT)
                                          .contentType(MediaType.TEXT_PLAIN)
                                          .header(ContentDigestValidatorInterceptor.CONTENT_DIGEST,
                                                  contentDigestHeaderFor("sha-512", "SHA-512", "different-body"))
                                          .content(REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Digest validation failed"))
                .andExpect(jsonPath("$.detail").value("Body hash did not match for algorithm: sha-512"));
        }
    }

    @Test
    void enforcedRequestValidation_acceptsValidContentDigestAndAddsResponseContentDigest() throws Exception {
        try (TestMvcContext context = testMvc(true, true)) {
            MvcResult result = context.mockMvc().perform(post(ENDPOINT)
                                                             .contentType(MediaType.TEXT_PLAIN)
                                                             .header(ContentDigestValidatorInterceptor.CONTENT_DIGEST,
                                                                     contentDigestHeaderFor("sha-512", "SHA-512",
                                                                                            REQUEST_BODY))
                                                             .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andReturn();

            assertThat(result.getResponse().getHeader(ContentDigestValidatorInterceptor.CONTENT_DIGEST))
                .isEqualTo(contentDigestHeaderFor("sha-512", "SHA-512",
                                                  result.getResponse().getContentAsByteArray()));
        }
    }

    @Test
    void enforcedRequestValidation_rejectsUnsupportedContentDigestAlgorithm() throws Exception {
        try (TestMvcContext context = testMvc(true, true)) {
            context.mockMvc().perform(post(ENDPOINT)
                                          .contentType(MediaType.TEXT_PLAIN)
                                          .header(ContentDigestValidatorInterceptor.CONTENT_DIGEST,
                                                  contentDigestHeaderFor("sha-256", "SHA-256", REQUEST_BODY))
                                          .content(REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Digest validation failed"))
                .andExpect(jsonPath("$.detail").value(
                    "Unsupported digest algorithm: sha-256. Supported algorithms (sha-512)."))
                .andExpect(jsonPath("$.supported_algorithms[0]").value("sha-512"));
        }
    }

    @Test
    void enforcedRequestValidation_rejectsMissingContentDigest() throws Exception {
        try (TestMvcContext context = testMvc(true, true)) {
            context.mockMvc().perform(post(ENDPOINT)
                                          .contentType(MediaType.TEXT_PLAIN)
                                          .content(REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing/Blank Content-Digest header"))
                .andExpect(jsonPath("$.detail").value(
                    "The Content-Digest header must be provided with a non blank value."));
        }
    }

    @Test
    void requestEnforceProperty_rejectsMissingContentDigest() throws Exception {
        try (TestMvcContext context = testMvc(true, true, false)) {
            context.mockMvc().perform(post(ENDPOINT)
                                          .contentType(MediaType.TEXT_PLAIN)
                                          .content(REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing/Blank Content-Digest header"))
                .andExpect(jsonPath("$.detail").value(
                    "The Content-Digest header must be provided with a non blank value."));
        }
    }

    @Test
    void requestEnforceProperty_acceptsValidContentDigestAndAddsResponseContentDigest() throws Exception {
        try (TestMvcContext context = testMvc(true, true, false)) {
            MvcResult result = context.mockMvc().perform(post(ENDPOINT)
                                                             .contentType(MediaType.TEXT_PLAIN)
                                                             .header(ContentDigestValidatorInterceptor.CONTENT_DIGEST,
                                                                     contentDigestHeaderFor("sha-512", "SHA-512",
                                                                                            REQUEST_BODY))
                                                             .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andReturn();

            assertThat(result.getResponse().getHeader(ContentDigestValidatorInterceptor.CONTENT_DIGEST))
                .isEqualTo(contentDigestHeaderFor("sha-512", "SHA-512",
                                                  result.getResponse().getContentAsByteArray()));
        }
    }

    private static TestMvcContext defaultTestMvc() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestMvcConfiguration.class);
        context.refresh();

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean(RequestCachingFilter.class),
                        context.getBean(ContentDigestResponseFilter.class))
            .build();
        return new TestMvcContext(context, mockMvc);
    }

    private static TestMvcContext testMvc(boolean requestAutoGenerate, boolean responseEnforce) {
        return testMvc(requestAutoGenerate, false, responseEnforce);
    }

    private static TestMvcContext testMvc(boolean requestAutoGenerate, boolean requestEnforce,
                                          boolean responseEnforce) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertyValues.of("opal.common.content-digest.request.auto-generate=" + requestAutoGenerate,
                              "opal.common.content-digest.request.enforce=" + requestEnforce,
                              "opal.common.content-digest.supported-algorithms.sha-512=SHA-512",
                              "opal.common.content-digest.response.enforce=" + responseEnforce,
                              "opal.common.content-digest.response.algorithm=SHA-512").applyTo(context);
        context.register(TestMvcConfiguration.class);
        context.refresh();

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean(RequestCachingFilter.class),
                        context.getBean(ContentDigestResponseFilter.class))
            .build();
        return new TestMvcContext(context, mockMvc);
    }

    private static String contentDigestHeaderFor(String rfcAlgorithm, String jcaAlgorithm, String content)
        throws Exception {
        return contentDigestHeaderFor(rfcAlgorithm, jcaAlgorithm, content.getBytes(StandardCharsets.UTF_8));
    }

    private static String contentDigestHeaderFor(String rfcAlgorithm, String jcaAlgorithm, byte[] content)
        throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance(jcaAlgorithm);
        String digest = Base64.getEncoder().encodeToString(messageDigest.digest(content));
        return rfcAlgorithm + "=:" + digest + ":";
    }

    private record TestMvcContext(AnnotationConfigWebApplicationContext applicationContext, MockMvc mockMvc)
        implements AutoCloseable {

        @Override
        public void close() {
            applicationContext.close();
        }
    }

    @Configuration
    @EnableWebMvc
    @Import({
        ContentDigestConfiguration.class,
        OpalGlobalExceptionHandler.class,
        TestController.class
    })
    static class TestMvcConfiguration {
    }

    @RestController
    static class TestController {

        @PostMapping(value = ENDPOINT, consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
        String post(@RequestBody String requestBody) {
            return RESPONSE_BODY;
        }
    }
}
