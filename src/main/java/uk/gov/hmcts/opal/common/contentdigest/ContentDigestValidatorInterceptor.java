package uk.gov.hmcts.opal.common.contentdigest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j(topic = "opal.ContentDigestValidatorInterceptor")
@Getter
public class ContentDigestValidatorInterceptor implements HandlerInterceptor {

    public static final String CONTENT_DIGEST = "Content-Digest";

    private static final Pattern ENTRY_PATTERN =
        Pattern.compile("(?i)\\s*([a-z0-9-]+)\\s*=\\s*:(?<b64>[A-Za-z0-9+/=]+):\\s*");

    private final boolean enforce;
    private final Map<String, String> rfcToJca; // RFC token -> JCA name
    private final List<String> supportedAlgorithms;

    public ContentDigestValidatorInterceptor(ContentDigestProperties properties) {
        this.enforce = properties.getRequest().isEnforced() || properties.getResponse().isEnforced();
        this.rfcToJca = properties.getSupportedAlgorithms().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                entry -> entry.getKey().toLowerCase(Locale.ROOT),
                Map.Entry::getValue));
        this.supportedAlgorithms = rfcToJca.keySet().stream().sorted().toList();
        log.info("Content-Digest enforce={}, algorithms={}", enforce, rfcToJca);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldSkipValidation(request)) {
            return true;
        }

        if (!(request instanceof CachedBodyHttpServletRequest cachedRequest)) {
            throw new InvalidContentDigestException("Content-Digest configuration error",
                                                    "Request body was not cached before Content-Digest validation.");
        }

        byte[] body = cachedRequest.getCachedBody();
        ContentDigest contentDigest = new ContentDigest(getAndValidateContentDigestHeader(request));

        byte[] expectedBytes = contentDigest.decodeSfBinary();
        MessageDigest md = contentDigest.getMessageDigest();

        byte[] actualBytes = md.digest(body);
        if (!MessageDigest.isEqual(actualBytes, expectedBytes)) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "Content-Digest verification failed for algorithms: {}. Expected: {}, Actual: {}",
                    contentDigest.getAlgorithm(),
                    Base64.getEncoder().encodeToString(expectedBytes),
                    Base64.getEncoder().encodeToString(actualBytes));
            }
            throw new InvalidContentDigestException("Digest validation failed",
                                                    "Body hash did not match for algorithm: "
                                                        + contentDigest.getAlgorithm());
        }

        log.debug("Content-Digest verification passed for algorithms: {}", contentDigest.getAlgorithm());
        return true;
    }

    private boolean shouldSkipValidation(HttpServletRequest request) {
        if (enforce) {
            return false;
        }

        String contentDigestHeader = request.getHeader(CONTENT_DIGEST);
        return contentDigestHeader == null || contentDigestHeader.isBlank();
    }

    String getAndValidateContentDigestHeader(HttpServletRequest request) {
        String contentDigest = request.getHeader(CONTENT_DIGEST);
        if (contentDigest == null || contentDigest.isBlank()) {
            throw new InvalidContentDigestException("Missing/Blank Content-Digest header",
                                                    "The Content-Digest header must be provided with a non blank "
                                                        + "value.");
        }
        return contentDigest;
    }

    @RequiredArgsConstructor
    @Getter
    public class ContentDigest {

        private final String algorithm;
        private final String base64;

        public ContentDigest(String contentDigest) {
            if (contentDigest.contains(",")) {
                throw new InvalidContentDigestException("Invalid Content-Digest header",
                                                        "Multiple digest entries are not supported.");
            }

            Matcher matcher = ENTRY_PATTERN.matcher(contentDigest);
            if (!matcher.matches()) {
                throw new InvalidContentDigestException("Invalid Content-Digest header",
                                                        "No valid digest entries found in header.");
            }

            this.algorithm = matcher.group(1).toLowerCase(Locale.ROOT);
            this.base64 = matcher.group("b64");
            validate();
        }

        void validate() {
            if (getAlgorithm() == null || getAlgorithm().isBlank()) {
                throw new InvalidContentDigestException("Digest validation failed",
                                                        "Digest algorithm is missing or blank.");
            }
            if (!supportedAlgorithms.contains(getAlgorithm())) {
                throw new InvalidContentDigestException("Digest validation failed",
                                                        "Unsupported digest algorithm: " + getAlgorithm()
                                                            + ". Supported algorithms ("
                                                            + String.join(",", supportedAlgorithms) + ").",
                                                        supportedAlgorithms);
            }
        }

        public MessageDigest getMessageDigest() {
            try {
                return MessageDigest.getInstance(rfcToJca.get(getAlgorithm()));
            } catch (Exception exception) {
                throw new InvalidContentDigestException("Digest validation failed",
                                                        "Unsupported digest algorithm: " + getAlgorithm()
                                                            + ". Supported algorithms ("
                                                            + String.join(",", supportedAlgorithms) + ").",
                                                        supportedAlgorithms);
            }
        }

        byte[] decodeSfBinary() {
            try {
                String base64Value = getBase64();
                int mod = base64Value.length() % 4;
                if (mod != 0) {
                    base64Value = base64Value + "===".substring(mod - 1);
                }
                return Base64.getDecoder().decode(base64Value);
            } catch (Exception e) {
                throw new InvalidContentDigestException("Digest validation failed",
                                                        "Bad base64 encoding for algorithm: " + getAlgorithm());
            }
        }
    }
}
