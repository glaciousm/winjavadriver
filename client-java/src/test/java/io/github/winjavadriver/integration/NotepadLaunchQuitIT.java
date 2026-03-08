package io.github.winjavadriver.integration;

import io.github.winjavadriver.WinJavaDriver;
import io.github.winjavadriver.WinJavaDriverService;
import io.github.winjavadriver.WinJavaOptions;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Phase 2: Launch Notepad, get session, quit.
 * This test requires the server to be built and notepad.exe available on the system.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotepadLaunchQuitIT {

    private static WinJavaDriverService service;

    @BeforeAll
    static void startService() throws IOException {
        File executable = findExecutable();

        service = new WinJavaDriverService.Builder()
                .usingDriverExecutable(executable)
                .usingPort(9517) // Use different port from StatusEndpointTest
                .withVerboseLogging(true)
                .build();

        service.start();
    }

    @AfterAll
    static void stopService() {
        if (service != null) {
            service.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should launch Notepad and create session")
    void shouldLaunchNotepadAndCreateSession() {
        WinJavaOptions options = new WinJavaOptions()
                .setApp("notepad.exe")
                .setWaitForAppLaunch(10);

        WinJavaDriver driver = new WinJavaDriver(service.getUrl(), options);

        try {
            // Verify session was created
            assertThat(driver.getSessionId()).isNotNull();
            assertThat(driver.getSessionId().toString()).isNotEmpty();
            System.out.println("Session created: " + driver.getSessionId());

            // Give Notepad a moment to fully render
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Clean up
            driver.quit();
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should quit and close Notepad")
    void shouldQuitAndCloseNotepad() {
        WinJavaOptions options = new WinJavaOptions()
                .setApp("notepad.exe")
                .setWaitForAppLaunch(10)
                .setShouldCloseApp(true);

        WinJavaDriver driver = new WinJavaDriver(service.getUrl(), options);
        String sessionId = driver.getSessionId().toString();
        assertThat(sessionId).isNotNull();

        // Quit should close the app
        driver.quit();

        // Verify by checking the session is gone (trying to use it would fail)
        // For now, we just verify quit() completed without exception
        System.out.println("Session " + sessionId + " closed successfully");
    }

    private static File findExecutable() {
        Path basePath = Path.of(System.getProperty("user.dir"));

        File exePath = basePath.resolve("../server/WinJavaDriver/bin/Release/net8.0-windows/win-x64/publish/winjavadriver.exe").toFile();
        if (exePath.exists()) {
            return exePath;
        }

        return new File("winjavadriver.exe");
    }
}
