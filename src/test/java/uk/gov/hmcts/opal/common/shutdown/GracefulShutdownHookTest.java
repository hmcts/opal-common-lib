package uk.gov.hmcts.opal.common.shutdown;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

class GracefulShutdownHookTest {

    private GracefulShutdownHook gracefulShutdownHook;
    private ServletWebServerApplicationContext applicationContext;

    @BeforeEach
    void beforeEach() {
        applicationContext = mock(ServletWebServerApplicationContext.class);
        gracefulShutdownHook = spy(new GracefulShutdownHook(applicationContext));
    }

    @Test
    void run_shouldSetReadinessToFalseAndDelayShutDownAndShutdownApplication() {
        doNothing().when(gracefulShutdownHook).setReadinessToFalse();
        doNothing().when(gracefulShutdownHook).delayShutdown();
        doNothing().when(gracefulShutdownHook).shutdownApplication();

        gracefulShutdownHook.run();
        verify(gracefulShutdownHook).setReadinessToFalse();
        verify(gracefulShutdownHook).delayShutdown();
        verify(gracefulShutdownHook).shutdownApplication();
    }

    @Test
    void setReadinessToFalse_shouldSetHealthCheckBeanReadyToFalse() {
        GracefulShutdownHealthCheck probeControllers = mock(GracefulShutdownHealthCheck.class);
        when(applicationContext.getBean("GracefulShutdownHealthCheck", GracefulShutdownHealthCheck.class))
            .thenReturn(probeControllers);
        gracefulShutdownHook.setReadinessToFalse();
        verify(probeControllers).setReady(false);
    }

    @Test
    void delayShutdown_shouldSleepForConfiguredTime() {
        String waitTimeString = "2s";
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(applicationContext.getBeanFactory()).thenReturn(beanFactory);

        when(beanFactory.resolveEmbeddedValue(any())).thenReturn(waitTimeString);

        long waitStartTime = System.currentTimeMillis();

        gracefulShutdownHook.delayShutdown();
        long waitEndTime = System.currentTimeMillis();
        long waitDuration = waitEndTime - waitStartTime;

        assertTrue(waitDuration >= 2000,
                   "Wait duration was less than 2000ms actual: " + waitDuration + "ms");

        verify(applicationContext).getBeanFactory();
        verify(beanFactory).resolveEmbeddedValue("${opal.common.shutdown.wait-time:30s}");
    }

    @Test
    void shutdownComplete_shouldCloseApplicationContext() {
        WebServer webServer = mock(WebServer.class);
        when(applicationContext.getWebServer()).thenReturn(webServer);

        gracefulShutdownHook.shutdownApplication();

        verify(applicationContext).getWebServer();
        verify(webServer).shutDownGracefully(gracefulShutdownHook);
    }

    @Test
    void shutdownComplete_shouldCloseApplicationContextAndLogResult() {
        GracefulShutdownResult result = mock(GracefulShutdownResult.class);
        gracefulShutdownHook.shutdownComplete(result);

        verify(applicationContext).close();
    }
}
