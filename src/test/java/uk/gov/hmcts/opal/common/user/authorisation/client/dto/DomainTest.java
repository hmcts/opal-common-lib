package uk.gov.hmcts.opal.common.user.authorisation.client.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainTest {

    @Test
    void findByDisplayName_returnsMatchingDomain() {
        assertThat(Domain.findByDisplayName("fines")).isEqualTo(Domain.FINES);
    }

    @Test
    void findByDisplayName_throwsWhenDisplayNameIsUnknown() {
        assertThatThrownBy(() -> Domain.findByDisplayName("unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown domain display name: unknown");
    }
}
