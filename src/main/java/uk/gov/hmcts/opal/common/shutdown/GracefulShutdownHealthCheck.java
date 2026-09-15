package uk.gov.hmcts.opal.common.shutdown;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("GracefulShutdownHealthCheck")
@Slf4j
public class GracefulShutdownHealthCheck implements HealthIndicator {
    public static final String HEALTH_KEY = "GracefulShutdown";

    @Getter
    private Health healthResult;

    @Override
    public Health health() {
        return healthResult;
    }

    public void setReady(boolean ready) {
        log.info("Updating application graceful shutdown health check state to: {}", ready ? "up" : "down");
        if (ready) {
            healthResult = new Health.Builder().withDetail(HEALTH_KEY, "application up").up().build();
        } else {
            healthResult = new Health.Builder().withDetail(HEALTH_KEY, "gracefully shutting down").down().build();
        }
    }

    @PostConstruct
    public void postConstruct() {
        setReady(true);
    }
}
