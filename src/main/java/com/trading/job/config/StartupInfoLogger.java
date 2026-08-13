package com.trading.job.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 【職責】應用就緒後於 Console 印出常用 URL，並對 HTTP 入口探測 UP／DOWN。
 * 【技巧】聽 {@link ApplicationReadyEvent}；開關來自 {@code startup.info.*}；UTF-8 {@link PrintStream}；
 *         探測超時短（800ms），失敗當 DOWN，不擋啟動。
 * 【概念】開發便利＋Loop 驗服務：bootRun 後應看到框線與本服務 [UP]。關聯服務未起則 [DOWN]（提示，非失敗）。
 * 【邊界】不負責啟動下游／Docker；不把探測當 Gate（Gate 仍是 {@code scripts/check.ps1}）。
 * 編碼：JVM {@code -Dstdout.encoding=UTF-8} + IDE Console UTF-8
 * （見 eos-minimal/knowledge/startup-info-logger-encoding.md）
 */
@Component
public class StartupInfoLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupInfoLogger.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        if (Boolean.FALSE.equals(env.getProperty("startup.info.enabled", Boolean.class, true))) {
            return;
        }

        String project = env.getProperty("startup.info.project-name", "TradingJob");
        String port = env.getProperty("server.port", "8084");
        String base = "http://localhost:" + port;
        String frontend = env.getProperty("startup.info.frontend", "none");
        boolean auth = Boolean.TRUE.equals(env.getProperty("startup.info.auth", Boolean.class, false));
        boolean h2 = !Boolean.FALSE.equals(env.getProperty("startup.info.h2", Boolean.class, true));
        boolean apiDocs = !Boolean.FALSE.equals(env.getProperty("startup.info.api-docs", Boolean.class, true));
        boolean probe = Boolean.TRUE.equals(env.getProperty("startup.info.probe", Boolean.class, true));

        PrintStream out = utf8Out();
        out.println();
        out.println("╔════════════════════════════════════════════════════════════════════════╗");
        out.printf("║  %-70s║%n", project + " 後端已啟動 — 使用連結");
        out.println("╠════════════════════════════════════════════════════════════════════════╣");
        out.println("║ 【後端 API / 工具】                                                      ║");
        link(out, probe, "健康檢查", base + "/actuator/health");
        link(out, probe, "應用資訊", base + "/actuator/info");
        if (apiDocs) {
            link(out, probe, "Swagger UI", base + "/swagger-ui.html");
            link(out, probe, "OpenAPI JSON",
                    base + env.getProperty("springdoc.api-docs.path", "/v3/api-docs"));
        }
        if (h2) {
            link(out, probe, "H2 Console", base + "/h2-console");
            String jdbc = env.getProperty("spring.datasource.url", "jdbc:h2:mem:tradingjob");
            out.printf("║   H2 JDBC URL  %s  帳號 sa  密碼 (空白)%n", jdbc);
        }

        if (!"none".equalsIgnoreCase(frontend)) {
            out.println("╠════════════════════════════════════════════════════════════════════════╣");
            if ("static".equalsIgnoreCase(frontend)) {
                out.println("║ 【前台】同埠靜態資源                                                      ║");
                link(out, probe, "首頁", base + env.getProperty("startup.info.home-path", "/"));
                for (String path : extraPaths(env)) {
                    link(out, probe, "額外", base + path);
                }
            } else if ("vite".equalsIgnoreCase(frontend)) {
                String feBase = "http://localhost:" + env.getProperty("startup.info.frontend-port", "5173");
                out.println("║ 【前台 Vue】需另執行 Frontend (Vite) 或 Full Stack                         ║");
                if (auth) {
                    link(out, probe, "登入頁", feBase + env.getProperty("startup.info.login-path", "/login"));
                }
                link(out, probe, "主頁", feBase + env.getProperty("startup.info.home-path", "/"));
            }
            if (auth) {
                out.printf("║   預設帳號     %s / %s%n",
                        env.getProperty("startup.info.default-user", "admin"),
                        env.getProperty("startup.info.default-pass", "admin123"));
            }
        }

        if ("none".equalsIgnoreCase(frontend)) {
            List<String> extras = extraPaths(env);
            if (!extras.isEmpty()) {
                for (String path : extras) {
                    link(out, probe, "額外", base + path);
                }
            }
        }

        List<String> related = relatedUrls(env);
        if (!related.isEmpty()) {
            out.println("╠════════════════════════════════════════════════════════════════════════╣");
            out.println("║ 【關聯服務】（未啟動則 DOWN，不擋本服務）                                   ║");
            for (String item : related) {
                int bar = item.indexOf('|');
                if (bar <= 0 || bar >= item.length() - 1) {
                    continue;
                }
                link(out, probe, item.substring(0, bar).trim(), item.substring(bar + 1).trim());
            }
        }

        out.println("╚════════════════════════════════════════════════════════════════════════╝");
        out.println();
        log.info("{} ready - frontend={} probe={} | {}", project, frontend, probe, base + "/actuator/health");
    }

    private static void link(PrintStream out, boolean probe, String label, String url) {
        String mark = "";
        if (probe && url != null && url.startsWith("http")) {
            mark = "  [" + (isUp(url) ? "UP" : "DOWN") + "]";
        }
        out.printf("║   %-12s %s%s%n", label, url, mark);
    }

    private static boolean isUp(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(800);
            conn.setReadTimeout(800);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            return code >= 200 && code < 500;
        } catch (Exception ex) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** UTF-8 banner；需 JVM stdout.encoding=UTF-8 與 IDE Console UTF-8。 */
    private static PrintStream utf8Out() {
        return new PrintStream(System.out, true, StandardCharsets.UTF_8);
    }

    private static List<String> extraPaths(Environment env) {
        return indexedOrCsv(env, "startup.info.extra-paths");
    }

    private static List<String> relatedUrls(Environment env) {
        return indexedOrCsv(env, "startup.info.related-urls");
    }

    private static List<String> indexedOrCsv(Environment env, String key) {
        String first = env.getProperty(key + "[0]");
        if (first != null && !first.isBlank()) {
            List<String> values = new ArrayList<>();
            for (int i = 0; ; i++) {
                String p = env.getProperty(key + "[" + i + "]");
                if (p == null || p.isBlank()) {
                    break;
                }
                String v = p.trim();
                if (key.endsWith("extra-paths")) {
                    v = v.startsWith("/") ? v : "/" + v;
                }
                values.add(v);
            }
            return values;
        }
        String raw = env.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> key.endsWith("extra-paths") && !s.startsWith("/") && !s.contains("|") ? "/" + s : s)
                .toList();
    }
}
