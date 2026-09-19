package uk.gov.hmcts.opal.common.shutdown;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

@RequiredArgsConstructor
@Slf4j
public class GracefulShutdownHook
    implements Runnable, GracefulShutdownCallback {

    private final ServletWebServerApplicationContext applicationContext;

    @Override
    public void run() {
        setReadinessToFalse();
        delayShutdown();
        shutdownApplication();
    }

    void setReadinessToFalse() {
        log.info("Setting readiness for application to false, "
            + "so the application doesn't receive new connections from now on.");
        GracefulShutdownHealthCheck probeControllers = applicationContext.getBean(
            "GracefulShutdownHealthCheck", GracefulShutdownHealthCheck.class);
        probeControllers.setReady(false);
    }

    //Required for graceful shutdown. Health check fails for a short time,
    // so the load balancer stops sending new requests.
    @SuppressWarnings("PMD.DoNotUseThreads")
    void delayShutdown() {
        try {
            String waitTimeString =
                applicationContext.getBeanFactory().resolveEmbeddedValue("${opal.common.shutdown.wait-time:30s}");
            Duration waitTime = DurationStyle.detectAndParse(waitTimeString);
            log.info("Waiting {} before shutdown down SpringContext! "
                    + "To allow time for the load balancer to stop sending new requests.",
                waitTime);
            Thread.sleep(waitTime.toMillis());
        } catch (InterruptedException e) {
            log.error("Error while gracefulshutdown Thread.sleep", e);
            Thread.currentThread().interrupt();
        }
    }

    void shutdownApplication() {
        log.info("Shutting down Application");
        //First shutdown the web server, so it stops accepting new connections
        applicationContext.getWebServer().shutDownGracefully(this);
        //Then shutdown application context in shutdown callback
    }

    @Override
    public void shutdownComplete(GracefulShutdownResult result) {
        applicationContext.close();
        log.info("Graceful shutdown complete: {}", result);
    }
}
