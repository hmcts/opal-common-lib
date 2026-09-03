package uk.gov.hmcts.opal.common.user.authentication.service;

import java.util.Optional;
import lombok.Getter;

@Getter
public enum SystemUserEnum {
    OPAL_SYSTEM_USER("opal-system-user");

    private final String configKey;

    SystemUserEnum(String configKey) {
        this.configKey = configKey;
    }

    public static Optional<SystemUserEnum> fromConfigKey(String key) {
        for (SystemUserEnum systemUserEnum : SystemUserEnum.values()) {
            if (systemUserEnum.getConfigKey().equals(key)) {
                return Optional.of(systemUserEnum);
            }
        }
        return Optional.empty();
    }
}
