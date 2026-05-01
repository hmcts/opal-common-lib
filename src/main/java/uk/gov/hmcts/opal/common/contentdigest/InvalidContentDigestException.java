package uk.gov.hmcts.opal.common.contentdigest;

import lombok.Getter;

import java.util.List;

@Getter
public class InvalidContentDigestException extends RuntimeException {

    private final String title;
    private final String detail;
    private final List<String> supportedAlgorithms;

    public InvalidContentDigestException(String title, String detail) {
        this(title, detail, List.of());
    }

    public InvalidContentDigestException(String title, String detail, List<String> supportedAlgorithms) {
        super(String.format("%s. %s", title, detail));
        this.title = title;
        this.detail = detail;
        this.supportedAlgorithms = List.copyOf(supportedAlgorithms);
    }
}
