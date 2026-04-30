package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Domain {
    FINES(1, "fines"),
    CONFISCATION(2, "confiscation"),
    MAINTENANCE(3, "maintenance");

    private final Integer databaseId;
    private final String displayName;

    Domain(Integer databaseId, String displayName) {
        this.databaseId = databaseId;
        this.displayName = displayName;
    }

    @JsonValue
    public String toJson() {
        return displayName;
    }

    public static Domain findByDisplayName(String displayName) {
        for (Domain domain : values()) {
            if (domain.displayName.equalsIgnoreCase(displayName)) {
                return domain;
            }
        }
        throw new IllegalArgumentException("Unknown domain display name: " + displayName);
    }
}
