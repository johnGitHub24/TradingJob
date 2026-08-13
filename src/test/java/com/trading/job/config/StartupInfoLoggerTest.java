package com.trading.job.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 【職責】驗證啟動橫幅：關閉不印；開啟時印 health／框線；probe=false 不打網路。
 * 【技巧】Mockito Environment；stdout 以 UTF-8 捕捉。
 * 【概念】Loop：bootRun 應見「後端已啟動」與本服務 [UP]；單元測不依賴已啟動的埠。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StartupInfoLogger 單元測試")
class StartupInfoLoggerTest {

    @Mock
    private ApplicationReadyEvent event;

    @Mock
    private ConfigurableApplicationContext applicationContext;

    @Mock
    private ConfigurableEnvironment env;

    private final StartupInfoLogger logger = new StartupInfoLogger();

    @Test
    @DisplayName("enabled=false → 不印")
    void disabled_printsNothing() {
        when(event.getApplicationContext()).thenReturn(applicationContext);
        when(applicationContext.getEnvironment()).thenReturn(env);
        when(env.getProperty("startup.info.enabled", Boolean.class, true)).thenReturn(false);

        String out = captureStdout(() -> logger.onApplicationEvent(event));

        assertThat(out).doesNotContain("後端已啟動");
    }

    @Test
    @DisplayName("enabled + probe=false → 印 health／框線，無 UP/DOWN")
    void enabled_printsBannerWithoutProbe() {
        when(event.getApplicationContext()).thenReturn(applicationContext);
        when(applicationContext.getEnvironment()).thenReturn(env);
        when(env.getProperty("startup.info.enabled", Boolean.class, true)).thenReturn(true);
        when(env.getProperty("startup.info.project-name", "TradingJob")).thenReturn("TradingJob");
        when(env.getProperty("server.port", "8084")).thenReturn("8084");
        when(env.getProperty("startup.info.frontend", "none")).thenReturn("none");
        when(env.getProperty("startup.info.auth", Boolean.class, false)).thenReturn(false);
        when(env.getProperty("startup.info.h2", Boolean.class, true)).thenReturn(true);
        when(env.getProperty("startup.info.api-docs", Boolean.class, true)).thenReturn(true);
        when(env.getProperty("startup.info.probe", Boolean.class, true)).thenReturn(false);
        when(env.getProperty("spring.datasource.url", "jdbc:h2:mem:tradingjob")).thenReturn("jdbc:h2:mem:tradingjob");
        when(env.getProperty("startup.info.extra-paths[0]")).thenReturn(null);
        when(env.getProperty("startup.info.extra-paths")).thenReturn(null);
        when(env.getProperty("startup.info.related-urls[0]")).thenReturn(null);
        when(env.getProperty("startup.info.related-urls")).thenReturn(null);

        String out = captureStdout(() -> logger.onApplicationEvent(event));

        assertThat(out).contains("TradingJob 後端已啟動");
        assertThat(out).contains("http://localhost:8084/actuator/health");
        assertThat(out).contains("╔");
        assertThat(out).doesNotContain("[UP]");
        assertThat(out).doesNotContain("[DOWN]");
    }

    private static String captureStdout(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(ps);
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
