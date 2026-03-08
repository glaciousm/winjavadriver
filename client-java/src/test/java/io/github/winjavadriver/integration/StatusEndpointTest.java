package io.github.winjavadriver.integration;

import io.github.winjavadriver.WinJavaDriverService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: Start service, call /status, verify response.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StatusEndpointTest {

    private static WinJavaDriverService service;
    private static HttpClient httpClient;

    @BeforeAll
    static void startService() throws IOException {
        File executable = findExecutable();

        service = new WinJavaDriverService.Builder()
                .usingDriverExecutable(executable)
                .usingPort(9516)
                .withVerboseLogging(true)
                .build();

        service.start();
        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    static void stopService() {
        if (service != null) {
            service.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Service should be running after start")
    void serviceShouldBeRunning() {
        assertThat(service.isRunning()).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("GET /status should return ready=true")
    void statusShouldReturnReady() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(service.getUrl() + "/status"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"ready\":true");
        assertThat(response.body()).contains("WinJavaDriver");
    }

    @Test
    @Order(3)
    @DisplayName("Unknown endpoint should return 404")
    void unknownEndpointShouldReturnError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(service.getUrl() + "/nonexistent"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("unknown command");
    }

    private static File findExecutable() {
        Path basePath = Path.of(System.getProperty("user.dir"));

        // Prefer published exe (Selenium DriverService requires a native executable)
        File exePath = basePath.resolve("../server/WinJavaDriver/bin/Release/net8.0-windows/win-x64/publish/winjavadriver.exe").toFile();
        if (exePath.exists()) {
            return exePath;
        }

        return new File("winjavadriver.exe");
    }
}
