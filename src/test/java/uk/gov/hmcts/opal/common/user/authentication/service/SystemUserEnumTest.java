package uk.gov.hmcts.opal.common.user.authentication.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemUserEnumTest {

    @Test
    void fromConfigKey_returnsMatchingSystemUserEnum() {
        assertThat(SystemUserEnum.fromConfigKey("opal-system-user"))
            .contains(SystemUserEnum.OPAL_SYSTEM_USER);
    }

    @Test
    void fromConfigKey_returnsEmptyWhenConfigKeyIsUnknown() {
        assertThat(SystemUserEnum.fromConfigKey("unknown"))
            .isEmpty();
    }

    @Test
    void fromConfigKey_returnsEmptyWhenConfigKeyIsNull() {
        assertThat(SystemUserEnum.fromConfigKey(null))
            .isEmpty();
    }
}

