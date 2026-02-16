package uk.gov.hmcts.opal.common.logging;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

@Service
public class ClockService {

    public LocalDateTime now() {
        return java.time.LocalDateTime.now();
    }
}
