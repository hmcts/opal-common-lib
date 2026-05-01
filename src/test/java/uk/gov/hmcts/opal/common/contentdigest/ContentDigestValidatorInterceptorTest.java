package uk.gov.hmcts.opal.common.contentdigest;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.opal.common.contentdigest.ContentDigestValidatorInterceptor.ContentDigest;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentDigestValidatorInterceptor")
class ContentDigestValidatorInterceptorTest {

    private static final Map<String, String> VALID_SUPPORTED_ALGORITHMS = Map.of("sha-512", "SHA-512");

    private static final String VALID_CONTENT_DIGEST_ALGORITHM = "sha-512";
    private static final String VALID_CONTENT_DIGEST_BASE64 =
        "EM2mJ8FHbS/CL6M4YidmWA3FiCxrgTGyaXLt+i1tP8qKebIXPraV1TIMtKYp9TsmwVGkhk0/EQ4pGqKSENY1pg==";
    private static final String VALID_CONTENT_DIGEST_HEADER =
        VALID_CONTENT_DIGEST_ALGORITHM + "=:" + VALID_CONTENT_DIGEST_BASE64 + ":";
    private static final String VALID_EMPTY_CONTENT_DIGEST_HEADER =
        VALID_CONTENT_DIGEST_ALGORITHM
            + "=:z4PhNX7vuL3xVChQ1m2AB9Yg5AULVxXcg/SpIdNs6c5H0NE8XYXysP+DGNKHfuwvY7kxvUdBeoGlODJ6+SfaPg==:";
    private static final String VALID_CONTENT_FOR_DIGEST = "request-body";

    private ContentDigestProperties getContentDigestProperties(boolean responseEnforce,
                                                               Map<String, String> supportedAlgorithms) {
        return getContentDigestProperties(false, responseEnforce, supportedAlgorithms);
    }

    private ContentDigestProperties getContentDigestProperties(boolean requestEnforce,
                                                               boolean responseEnforce,
                                                               Map<String, String> supportedAlgorithms) {
        ContentDigestProperties.Request request = new ContentDigestProperties.Request(requestEnforce, false);
        ContentDigestProperties.Response response = new ContentDigestProperties.Response(responseEnforce, "SHA-512");
        return new ContentDigestProperties(supportedAlgorithms, request, response);
    }

    private ContentDigestValidatorInterceptor getValidContentDigestValidatorInterceptor() {
        return new ContentDigestValidatorInterceptor(getContentDigestProperties(true,
                                                                                VALID_SUPPORTED_ALGORITHMS));
    }

    @DisplayName("Constructor initialization")
    @Test
    void constructorInitialization() {
        ContentDigestValidatorInterceptor interceptor = new ContentDigestValidatorInterceptor(
            getContentDigestProperties(true, Map.of("sha-256", "SHA-256", "sha-512", "SHA-512"))
        );

        assertThat(interceptor.isEnforce()).isTrue();
        assertThat(interceptor.getRfcToJca()).containsExactlyInAnyOrderEntriesOf(Map.of("sha-256", "SHA-256",
                                                                                        "sha-512", "SHA-512"));
        assertThat(interceptor.getSupportedAlgorithms()).containsExactly("sha-256", "sha-512");

        interceptor = new ContentDigestValidatorInterceptor(getContentDigestProperties(false,
                                                                                       VALID_SUPPORTED_ALGORITHMS));

        assertThat(interceptor.isEnforce()).isFalse();
        assertThat(interceptor.getRfcToJca()).containsExactlyInAnyOrderEntriesOf(VALID_SUPPORTED_ALGORITHMS);
        assertThat(interceptor.getSupportedAlgorithms()).containsExactly("sha-512");
    }

    @DisplayName("Should enable enforcement when request enforcement is enabled")
    @Test
    void shouldEnableEnforcementWhenRequestEnforcementIsEnabled() {
        ContentDigestValidatorInterceptor interceptor = new ContentDigestValidatorInterceptor(
            getContentDigestProperties(true, false, VALID_SUPPORTED_ALGORITHMS)
        );

        assertThat(interceptor.isEnforce()).isTrue();
    }

    @DisplayName("Should enable enforcement when response enforcement is enabled")
    @Test
    void shouldEnableEnforcementWhenResponseEnforcementIsEnabled() {
        ContentDigestValidatorInterceptor interceptor = new ContentDigestValidatorInterceptor(
            getContentDigestProperties(false, true, VALID_SUPPORTED_ALGORITHMS)
        );

        assertThat(interceptor.isEnforce()).isTrue();
    }

    @DisplayName("Should enable enforcement when request and response enforcement are enabled")
    @Test
    void shouldEnableEnforcementWhenRequestAndResponseEnforcementAreEnabled() {
        ContentDigestValidatorInterceptor interceptor = new ContentDigestValidatorInterceptor(
            getContentDigestProperties(true, true, VALID_SUPPORTED_ALGORITHMS)
        );

        assertThat(interceptor.isEnforce()).isTrue();
    }

    @DisplayName("Should disable enforcement when request and response enforcement are disabled")
    @Test
    void shouldDisableEnforcementWhenRequestAndResponseEnforcementAreDisabled() {
        ContentDigestValidatorInterceptor interceptor = new ContentDigestValidatorInterceptor(
            getContentDigestProperties(false, false, VALID_SUPPORTED_ALGORITHMS)
        );

        assertThat(interceptor.isEnforce()).isFalse();
    }

    @Nested
    @DisplayName("String getAndValidateContentDigestHeader(HttpServletRequest request)")
    class GetAndValidateContentDigestHeader {

        @DisplayName("Should throw exception when Content-Digest header is missing")
        @Test
        void shouldThrowExceptionWhenHeaderIsMissing() {
            CachedBodyHttpServletRequest request = mock(CachedBodyHttpServletRequest.class);

            ContentDigestValidatorInterceptor interceptor = getValidContentDigestValidatorInterceptor();
            assertThatThrownBy(() -> interceptor.getAndValidateContentDigestHeader(request))
                .isInstanceOf(InvalidContentDigestException.class)
                .hasFieldOrPropertyWithValue("title", "Missing/Blank Content-Digest header")
                .hasFieldOrPropertyWithValue("detail",
                    "The Content-Digest header must be provided with a non blank value.");
        }

        @DisplayName("Should throw exception when Content-Digest header is blank")
        @Test
        void shouldThrowExceptionWhenHeaderIsBlank() {
            CachedBodyHttpServletRequest request = mock(CachedBodyHttpServletRequest.class);
            lenient().doReturn("")
                .when(request).getHeader("Content-Digest");

            ContentDigestValidatorInterceptor interceptor = getValidContentDigestValidatorInterceptor();
            assertThatThrownBy(() -> interceptor.getAndValidateContentDigestHeader(request))
                .isInstanceOf(InvalidContentDigestException.class)
                .hasFieldOrPropertyWithValue("title", "Missing/Blank Content-Digest header")
                .hasFieldOrPropertyWithValue("detail",
                                             "The Content-Digest header must be provided with a non blank value.");
        }
    }

    @Nested
    @DisplayName("public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)")
    class PreHandle {

        private CachedBodyHttpServletRequest getValidRequest() {
            CachedBodyHttpServletRequest request = mock(CachedBodyHttpServletRequest.class);
            lenient().doReturn(VALID_CONTENT_DIGEST_HEADER)
                .when(request).getHeader("Content-Digest");
            lenient().doReturn(VALID_CONTENT_FOR_DIGEST.getBytes()).when(request).getCachedBody();
            return request;
        }

        private CachedBodyHttpServletRequest getRequestWithoutContentDigestHeader() {
            CachedBodyHttpServletRequest request = mock(CachedBodyHttpServletRequest.class);
            lenient().doReturn(VALID_CONTENT_FOR_DIGEST.getBytes()).when(request).getCachedBody();
            return request;
        }

        private CachedBodyHttpServletRequest getEmptyRequest(String contentDigestHeader) {
            CachedBodyHttpServletRequest request = mock(CachedBodyHttpServletRequest.class);
            lenient().doReturn(contentDigestHeader).when(request).getHeader("Content-Digest");
            lenient().doReturn(new byte[0]).when(request).getCachedBody();
            return request;
        }

        @DisplayName("Should skip validation when enforcement is disabled and Content-Digest header is missing")
        @Test
        void shouldSkipValidationWhenEnforcementIsDisabledAndContentDigestHeaderIsMissing() {
            ContentDigestValidatorInterceptor interceptor = spy(new ContentDigestValidatorInterceptor(
                getContentDigestProperties(false, VALID_SUPPORTED_ALGORITHMS)));
            CachedBodyHttpServletRequest request = getRequestWithoutContentDigestHeader();

            boolean result = interceptor.preHandle(request, null, null);

            assertThat(result).isTrue();
        }

        @DisplayName("Should skip validation for plain request when enforcement is disabled and header is missing")
        @Test
        void shouldSkipValidationForPlainRequestWhenEnforcementIsDisabledAndContentDigestHeaderIsMissing() {
            ContentDigestValidatorInterceptor interceptor = spy(new ContentDigestValidatorInterceptor(
                getContentDigestProperties(false, VALID_SUPPORTED_ALGORITHMS)));
            HttpServletRequest request = mock(HttpServletRequest.class);

            boolean result = interceptor.preHandle(request, null, null);

            assertThat(result).isTrue();
        }

        @DisplayName("Should throw content digest exception when validation requires uncached request body")
        @Test
        void shouldThrowContentDigestExceptionWhenValidationRequiresUncachedRequestBody() {
            ContentDigestValidatorInterceptor interceptor = spy(new ContentDigestValidatorInterceptor(
                getContentDigestProperties(true, VALID_SUPPORTED_ALGORITHMS)));
            HttpServletRequest request = mock(HttpServletRequest.class);

            assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOf(InvalidContentDigestException.class)
                .hasFieldOrPropertyWithValue("title", "Content-Digest configuration error")
                .hasFieldOrPropertyWithValue("detail",
                                             "Request body was not cached before Content-Digest validation.");
        }

        @DisplayName("Should reject empty body when enforcement is enabled and Content-Digest header is missing")
        @Test
        void shouldRejectEmptyBodyWhenEnforcementIsEnabledAndContentDigestHeaderIsMissing() {
            ContentDigestValidatorInterceptor interceptor = spy(new ContentDigestValidatorInterceptor(
                getContentDigestProperties(true, VALID_SUPPORTED_ALGORITHMS)));
            CachedBodyHttpServletRequest request = getEmptyRequest(null);

            assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOf(InvalidContentDigestException.class)
                .hasFieldOrPropertyWithValue("title", "Missing/Blank Content-Digest header")
                .hasFieldOrPropertyWithValue("detail",
                                             "The Content-Digest header must be provided with a non blank value.");
        }

        @DisplayName("Should pass validation for empty body when enforcement is enabled and Content-Digest is valid")
        @Test
        void shouldPassValidationForEmptyBodyWhenEnforcementIsEnabledAndContentDigestIsValid() {
            ContentDigestValidatorInterceptor interceptor = spy(new ContentDigestValidatorInterceptor(
                getContentDigestProperties(true, VALID_SUPPORTED_ALGORITHMS)));
            CachedBodyHttpServletRequest request = getEmptyRequest(VALID_EMPTY_CONTENT_DIGEST_HEADER);

            boolean result = interceptor.preHandle(request, null, null);

            assertThat(result).isTrue();
        }

        @DisplayName("Should fail validation for empty body when enforcement is enabled and Content-Digest is invalid")
        @Test
        void shouldFailValidationForEmptyBodyWhenEnforcementIsEnabledAndContentDigestIsInvalid() {
            ContentDigestValidatorInterceptor interceptor = spy(new ContentDigestValidatorInterceptor(
                getContentDigestProperties(true, VALID_SUPPORTED_ALGORITHMS)));
            CachedBodyHttpServletRequest request = getEmptyRequest(VALID_CONTENT_DIGEST_HEADER);

            assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOf(InvalidContentDigestException.class)
                .hasFieldOrPropertyWithValue("title", "Digest validation failed")
                .hasFieldOrPropertyWithValue("detail", "Body hash did not match for algorithm: sha-512");
        }

        @DisplayName("Should skip validation for empty body when enforcement is disabled and Content-Digest is missing")
        @Test
        void shouldSkipValidationForEmptyBodyWhenEnforcementIsDisabledAndContentDigestIsMissing() {
            ContentDigestValidatorInterceptor interceptor = spy(new ContentDigestValidatorInterceptor(
                getContentDigestProperties(false, VALID_SUPPORTED_ALGORITHMS)));
            CachedBodyHttpServletRequest request = getEmptyRequest(null);

            boolean result = interceptor.preHandle(request, null, null);

            assertThat(result).isTrue();
        }

        @DisplayName("Should pass validation when enforcement is disabled and Content-Digest is valid")
        @Test
        void shouldPassValidationWhenEnforcementIsDisabledAndContentDigestIsValid() {
            ContentDigestValidatorInterceptor interceptor = spy(new ContentDigestValidatorInterceptor(
                getContentDigestProperties(false, VALID_SUPPORTED_ALGORITHMS)));
            CachedBodyHttpServletRequest request = getValidRequest();

            boolean result = interceptor.preHandle(request, null, null);

            assertThat(result).isTrue();
        }

        @DisplayName("Should pass validation when Content-Digest is valid")
        @Test
        void shouldPassValidationWhenContentDigestIsValid() {
            ContentDigestValidatorInterceptor interceptor = spy(new ContentDigestValidatorInterceptor(
                getContentDigestProperties(true, VALID_SUPPORTED_ALGORITHMS)));
            CachedBodyHttpServletRequest request = getValidRequest();
            boolean result = interceptor.preHandle(request, null, null);
            assertThat(result).isTrue();
        }

        @DisplayName("Should fail validation when Content-Digest is invalid")
        @Test
        void shouldFailValidationWhenContentDigestIsInvalid() {
            ContentDigestValidatorInterceptor interceptor = spy(new ContentDigestValidatorInterceptor(
                getContentDigestProperties(true, VALID_SUPPORTED_ALGORITHMS)));
            CachedBodyHttpServletRequest request = getValidRequest();
            //Modify header to make it invalid
            lenient().doReturn("some-value-invalid".getBytes()).when(request).getCachedBody();
            assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOf(InvalidContentDigestException.class)
                .hasFieldOrPropertyWithValue("title", "Digest validation failed")
                .hasFieldOrPropertyWithValue("detail", "Body hash did not match for algorithm: sha-512");
        }

        @DisplayName("Should fail validation when enforcement is disabled and Content-Digest is invalid")
        @Test
        void shouldFailValidationWhenEnforcementIsDisabledAndContentDigestIsInvalid() {
            ContentDigestValidatorInterceptor interceptor = spy(new ContentDigestValidatorInterceptor(
                getContentDigestProperties(false, VALID_SUPPORTED_ALGORITHMS)));
            CachedBodyHttpServletRequest request = getValidRequest();
            lenient().doReturn("some-value-invalid".getBytes()).when(request).getCachedBody();

            assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOf(InvalidContentDigestException.class)
                .hasFieldOrPropertyWithValue("title", "Digest validation failed")
                .hasFieldOrPropertyWithValue("detail", "Body hash did not match for algorithm: sha-512");
        }
    }

    @Nested
    @DisplayName("ContentDigest")
    class ContentDigestTest {

        private ContentDigest createContentDigest(String contentDigest) {
            ContentDigestValidatorInterceptor interceptor = getValidContentDigestValidatorInterceptor();
            return interceptor.new ContentDigest(contentDigest);
        }

        private ContentDigest createContentDigest(String algorithm, String base64) {
            ContentDigestValidatorInterceptor interceptor = getValidContentDigestValidatorInterceptor();
            ContentDigest contentDigest = interceptor.new ContentDigest(VALID_CONTENT_DIGEST_HEADER);
            ReflectionTestUtils.setField(contentDigest, "algorithm", algorithm);
            ReflectionTestUtils.setField(contentDigest, "base64", base64);
            return contentDigest;
        }

        @Nested
        @DisplayName("public ContentDigest(String contentDigest)")
        class Constructor {

            @DisplayName("Should throw exception when multiple digest entries are provided")
            @Test
            void shouldThrowExceptionWhenMultipleDigestEntriesProvided() {
                assertThatThrownBy(() -> createContentDigest("someValue,someOtherValue"))
                    .isInstanceOf(InvalidContentDigestException.class)
                    .hasFieldOrPropertyWithValue("title", "Invalid Content-Digest header")
                    .hasFieldOrPropertyWithValue("detail", "Multiple digest entries are not supported.");
            }

            @DisplayName("Should throw exception when no valid digest entries are found")
            @Test
            void shouldThrowExceptionWhenNoValidDigestEntriesFound() {
                assertThatThrownBy(() -> createContentDigest("someInvalidPattern"))
                    .isInstanceOf(InvalidContentDigestException.class)
                    .hasFieldOrPropertyWithValue("title", "Invalid Content-Digest header")
                    .hasFieldOrPropertyWithValue("detail", "No valid digest entries found in header.");
            }

            @DisplayName("Should create ContentDigest when valid entry is provided")
            @Test
            void shouldCreateContentDigestWhenValidEntryProvided() {
                ContentDigest contentDigest = createContentDigest(VALID_CONTENT_DIGEST_HEADER);

                assertThat(contentDigest.getAlgorithm()).isEqualTo(VALID_CONTENT_DIGEST_ALGORITHM);
                assertThat(contentDigest.getBase64()).isEqualTo(VALID_CONTENT_DIGEST_BASE64);
            }
        }

        @Nested
        @DisplayName("void validate()")
        class Validate {

            @DisplayName("Should throw exception algorithm is missing")
            @Test
            void shouldThrowExceptionWhenAlgorithmIsMissing() {
                ContentDigest contentDigest = createContentDigest(null, VALID_CONTENT_DIGEST_BASE64);

                assertThatThrownBy(contentDigest::validate)
                    .isInstanceOf(InvalidContentDigestException.class)
                    .hasFieldOrPropertyWithValue("title", "Digest validation failed")
                    .hasFieldOrPropertyWithValue("detail", "Digest algorithm is missing or blank.");
            }

            @DisplayName("Should throw exception when algorithm is blank")
            @Test
            void shouldThrowExceptionWhenAlgorithmIsBlank() {
                ContentDigest contentDigest = createContentDigest("", VALID_CONTENT_DIGEST_BASE64);

                assertThatThrownBy(contentDigest::validate)
                    .isInstanceOf(InvalidContentDigestException.class)
                    .hasFieldOrPropertyWithValue("title", "Digest validation failed")
                    .hasFieldOrPropertyWithValue("detail", "Digest algorithm is missing or blank.");
            }

            @DisplayName("Should throw exception when algorithm is not supported")
            @Test
            void shouldThrowExceptionWhenAlgorithmNotSupported() {
                ContentDigest contentDigest = createContentDigest("unsupported-algorithm", VALID_CONTENT_DIGEST_BASE64);

                assertThatThrownBy(contentDigest::validate)
                    .isInstanceOf(InvalidContentDigestException.class)
                    .hasFieldOrPropertyWithValue("title", "Digest validation failed")
                    .hasFieldOrPropertyWithValue("detail",
                        "Unsupported digest algorithm: unsupported-algorithm. Supported algorithms (sha-512).")
                    .hasFieldOrPropertyWithValue("supportedAlgorithms", List.of("sha-512"));
            }

            @DisplayName("Should return supported algorithms in deterministic order when algorithm is not supported")
            @Test
            void shouldReturnSupportedAlgorithmsInDeterministicOrderWhenAlgorithmNotSupported() {
                ContentDigestValidatorInterceptor interceptor = new ContentDigestValidatorInterceptor(
                    getContentDigestProperties(true, Map.of("sha-512", "SHA-512", "sha-256", "SHA-256"))
                );

                ContentDigest contentDigest = interceptor.new ContentDigest(VALID_CONTENT_DIGEST_HEADER);
                ReflectionTestUtils.setField(contentDigest, "algorithm", "unsupported-algorithm");

                assertThatThrownBy(contentDigest::validate)
                    .isInstanceOf(InvalidContentDigestException.class)
                    .hasFieldOrPropertyWithValue("title", "Digest validation failed")
                    .hasFieldOrPropertyWithValue("detail",
                        "Unsupported digest algorithm: unsupported-algorithm. Supported algorithms (sha-256,sha-512).")
                    .hasFieldOrPropertyWithValue("supportedAlgorithms", List.of("sha-256", "sha-512"));
            }

            @DisplayName("Should pass validation when algorithm is supported")
            @Test
            void shouldPassValidationWhenAlgorithmSupported() {
                assertThatNoException().isThrownBy(() -> createContentDigest(VALID_CONTENT_DIGEST_HEADER).validate());
            }
        }

        @Nested
        @DisplayName("public MessageDigest getMessageDigest()")
        class GetMessageDigest {

            @DisplayName("Should throw exception when algorithm is not supported by JCA")
            @Test
            void shouldThrowExceptionWhenAlgorithmNotSupportedByJca() {
                ContentDigest contentDigest = createContentDigest("sha-256", VALID_CONTENT_DIGEST_BASE64);

                assertThatThrownBy(contentDigest::getMessageDigest)
                    .isInstanceOf(InvalidContentDigestException.class)
                    .hasFieldOrPropertyWithValue("title", "Digest validation failed")
                    .hasFieldOrPropertyWithValue("detail",
                        "Unsupported digest algorithm: sha-256. Supported algorithms (sha-512).")
                    .hasFieldOrPropertyWithValue("supportedAlgorithms", List.of("sha-512"));
            }

            @DisplayName("Should return MessageDigest when algorithm is supported by JCA")
            @Test
            void shouldReturnMessageDigestWhenAlgorithmSupportedByJca() {
                ContentDigest contentDigest = createContentDigest(VALID_CONTENT_DIGEST_HEADER);

                assertThatNoException().isThrownBy(contentDigest::getMessageDigest);
                assertThat(contentDigest.getMessageDigest().getAlgorithm()).isEqualTo("SHA-512");
            }
        }

        @Nested
        @DisplayName("byte[] decodeSfBinary(String base64NoColons)")
        class DecodeSfBinary {

            @DisplayName("Should throw exception when base64 is invalid")
            @Test
            void shouldThrowExceptionWhenBase64IsInvalid() {
                ContentDigest contentDigest = createContentDigest(VALID_CONTENT_DIGEST_ALGORITHM, "invalid-base64");

                assertThatThrownBy(contentDigest::decodeSfBinary)
                    .isInstanceOf(InvalidContentDigestException.class)
                    .hasFieldOrPropertyWithValue("title", "Digest validation failed")
                    .hasFieldOrPropertyWithValue("detail", "Bad base64 encoding for algorithm: sha-512");
            }

            @DisplayName("Should return decoded byte array when base64 is valid")
            @Test
            void shouldReturnDecodedByteArrayWhenBase64IsValid() {
                ContentDigest contentDigest = createContentDigest(VALID_CONTENT_DIGEST_HEADER);
                byte[] decoded = contentDigest.decodeSfBinary();

                assertThat(decoded)
                    .isNotNull()
                    .isEqualTo(Base64.getDecoder().decode(VALID_CONTENT_DIGEST_BASE64));
            }

            @DisplayName("Should pad and return decoded byte array when base64 is valid but missing padding")
            @Test
            void shouldPadAndReturnDecodedByteArrayWhenBase64IsValidButMissingPadding() {
                String base64MissingPadding = VALID_CONTENT_DIGEST_BASE64.replace("=", "");
                ContentDigest contentDigest = createContentDigest(VALID_CONTENT_DIGEST_ALGORITHM, base64MissingPadding);

                byte[] decoded = contentDigest.decodeSfBinary();

                assertThat(decoded)
                    .isNotNull()
                    .isEqualTo(Base64.getDecoder().decode(VALID_CONTENT_DIGEST_BASE64));
            }
        }
    }
}
