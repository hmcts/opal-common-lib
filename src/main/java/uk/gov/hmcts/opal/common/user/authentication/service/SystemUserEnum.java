package uk.gov.hmcts.opal.common.user.authentication.service;

import lombok.Getter;

@Getter
public enum SystemUserEnum {
    OPAL_SYSTEM_USER("opal-system-user");

    private final String configKey;

    SystemUserEnum(String configKey) {
        this.configKey = configKey;
    }
}
