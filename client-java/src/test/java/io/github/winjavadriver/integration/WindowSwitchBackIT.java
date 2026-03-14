package io.github.winjavadriver.integration;

import io.github.winjavadriver.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.NoSuchWindowException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for WinJavaDriver.switchBack() — window handle tracking.
 * Uses charmap.exe (Character Map), a classic Win32 app available on all Windows versions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WindowSwitchBackIT {

    private static WinJavaDriverService service;
    private WinJavaDriver driver;

    @BeforeAll
    static void startService() throws IOException {
        File executable = findExecutable();
        service = new WinJavaDriverService.Builder()
                .usingDriverExecutable(executable)
                .usingPort(9522)
                .withVerboseLogging(true)
                .build();
        service.start();
    }

    @AfterAll
    static void stopService() {
        if (service != null) service.stop();
    }

    @BeforeEach
    void launchApp() {
        WinJavaOptions options = new WinJavaOptions()
                .setApp("charmap.exe")
                .setWaitForAppLaunch(10)
                .setShouldCloseApp(true);
        driver = new WinJavaDriver(service.getUrl(), options);
    }

    @AfterEach
    void closeApp() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
    }

    @Test
    @Order(1)
    @DisplayName("switchBack with no previous window should throw NoSuchWindowException")
    void switchBackWithNoPreviousShouldThrow() {
        assertThatThrownBy(() -> driver.switchBack())
                .isInstanceOf(NoSuchWindowException.class)
                .hasMessageContaining("No previous window");
    }

    @Test
    @Order(2)
    @DisplayName("switchTo().window() should record previous handle for switchBack()")
    void shouldRecordPreviousHandle() {
        String handle = driver.getWindowHandle();
        assertThat(handle).isNotNull();

        // Switching to the same window still records the previous handle
        driver.switchTo().window(handle);

        // switchBack should succeed (returns to the same handle, but the mechanism works)
        driver.switchBack();
        assertThat(driver.getWindowHandle()).isEqualTo(handle);
    }

    @Test
    @Order(3)
    @DisplayName("switchBack should return this driver for chaining")
    void switchBackShouldReturnDriverForChaining() {
        String handle = driver.getWindowHandle();

        driver.switchTo().window(handle);
        WinJavaDriver returned = driver.switchBack();

        assertThat(returned).isSameAs(driver);
    }

    @Test
    @Order(4)
    @DisplayName("Consecutive switchBack should throw — previous is cleared after use")
    void consecutiveSwitchBackShouldThrow() {
        String handle = driver.getWindowHandle();

        driver.switchTo().window(handle);
        driver.switchBack(); // consumes the previous handle

        assertThatThrownBy(() -> driver.switchBack())
                .isInstanceOf(NoSuchWindowException.class);
    }

    @Test
    @Order(5)
    @DisplayName("Multiple switchTo calls should only track the last previous")
    void multipleSwitchesToShouldTrackLastPrevious() {
        String handle = driver.getWindowHandle();

        // Switch twice — second switch overwrites the tracked previous
        driver.switchTo().window(handle);
        driver.switchTo().window(handle);

        // switchBack should work (tracks the handle from the second switch)
        driver.switchBack();
        assertThat(driver.getWindowHandle()).isEqualTo(handle);
    }

    private static File findExecutable() {
        String sysProp = System.getProperty("webdriver.winjavadriver.driver");
        if (sysProp != null) {
            File f = new File(sysProp);
            if (f.exists()) return f;
        }

        Path basePath = Path.of(System.getProperty("user.dir"));
        String[] candidates = {
                "../server/WinJavaDriver/bin/Release/net8.0-windows/win-x64/publish/winjavadriver.exe",
                "../server/WinJavaDriver/bin/Release/net8.0-windows/win-x64/winjavadriver.exe",
                "../../winjavadriver-private/server/WinJavaDriver/bin/Release/net8.0-windows/win-x64/winjavadriver.exe",
        };
        for (String candidate : candidates) {
            File f = basePath.resolve(candidate).toAbsolutePath().normalize().toFile();
            if (f.exists()) return f;
        }
        return new File("winjavadriver.exe");
    }
}
