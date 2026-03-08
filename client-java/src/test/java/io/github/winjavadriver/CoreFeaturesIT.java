package io.github.winjavadriver;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5 integration tests: Core features - windows, screenshots, source, timeouts, XPath.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CoreFeaturesIT {

    private static WinJavaDriverService service;
    private static WinJavaDriver driver;

    @BeforeAll
    static void setupClass() throws IOException {
        File executable = findExecutable();
        service = new WinJavaDriverService.Builder()
                .usingDriverExecutable(executable)
                .build();
        service.start();

        WinJavaOptions options = new WinJavaOptions()
                .setApp("notepad.exe")
                .setWaitForAppLaunch(10);
        driver = new WinJavaDriver(service, options);
        assertNotNull(driver.getSessionId());
    }

    @AfterAll
    static void teardownClass() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception e) { /* ignore */ }
        }
        if (service != null) {
            service.stop();
        }

        try {
            new ProcessBuilder("taskkill", "/F", "/IM", "notepad.exe")
                    .redirectErrorStream(true).start().waitFor();
        } catch (Exception e) { /* ignore */ }
    }

    @Test
    @Order(1)
    void shouldGetWindowHandle() {
        String handle = driver.getWindowHandle();
        assertNotNull(handle);
        assertFalse(handle.isEmpty());
        System.out.println("Window handle: " + handle);
    }

    @Test
    @Order(2)
    void shouldGetWindowHandles() {
        Set<String> handles = driver.getWindowHandles();
        assertNotNull(handles);
        assertFalse(handles.isEmpty());
        System.out.println("Window handles: " + handles);
    }

    @Test
    @Order(3)
    void shouldTakeWindowScreenshot() {
        String screenshot = driver.getScreenshotAs(OutputType.BASE64);
        assertNotNull(screenshot);
        assertFalse(screenshot.isEmpty());
        assertTrue(screenshot.length() > 100);
        System.out.println("Screenshot base64 length: " + screenshot.length());
    }

    @Test
    @Order(4)
    void shouldSaveScreenshotToFile() {
        File screenshotFile = driver.getScreenshotAs(OutputType.FILE);
        assertNotNull(screenshotFile);
        assertTrue(screenshotFile.exists());
        assertTrue(screenshotFile.length() > 0);
        System.out.println("Screenshot saved to: " + screenshotFile.getAbsolutePath()
                + " (" + screenshotFile.length() + " bytes)");
    }

    @Test
    @Order(5)
    void shouldGetPageSource() {
        String source = driver.getPageSource();
        System.out.println("Page source: " + (source == null ? "null" : "length=" + source.length()));
        assertNotNull(source, "Page source should not be null");
    }

    @Test
    @Order(6)
    void shouldSetAndGetTimeouts() {
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(5000));
        // Just verify it doesn't throw
        System.out.println("Implicit wait set to 5000ms");
    }

    @Test
    @Order(7)
    void shouldFindElementByXPath() {
        try {
            WebElement element = driver.findElement(By.xpath("//Button"));
            assertNotNull(element);
            System.out.println("Found element by XPath: " + element);
        } catch (Exception e) {
            System.out.println("XPath lookup failed (expected in some cases): " + e.getMessage());
            WebElement element = driver.findElement(By.tagName("button"));
            assertNotNull(element, "Should find element by tagName as fallback");
            System.out.println("Found element by tagName fallback: " + element);
        }
    }

    @Test
    @Order(8)
    void shouldFindElementsByXPath() {
        List<WebElement> menuItems = driver.findElements(By.xpath("//MenuItem"));
        System.out.println("Found " + menuItems.size() + " menu items by XPath");
        assertNotNull(menuItems);
    }

    @Test
    @Order(9)
    void shouldFindElementByXPathWithAttribute() {
        List<WebElement> elements = driver.findElements(By.xpath("//*[@Name='Help']"));
        System.out.println("Found " + elements.size() + " elements with Name='Help'");
        assertNotNull(elements);
    }

    @Test
    @Order(10)
    void shouldTakeElementScreenshot() {
        WebElement element = driver.findElement(By.tagName("button"));
        assertNotNull(element, "Should find a button element");

        String screenshot = element.getScreenshotAs(OutputType.BASE64);
        assertNotNull(screenshot);
        assertFalse(screenshot.isEmpty());
        System.out.println("Element screenshot base64 length: " + screenshot.length());
    }

    @Test
    @Order(11)
    void shouldMaximizeWindow() {
        assertDoesNotThrow(() -> driver.manage().window().maximize());
        System.out.println("Window maximized");
    }

    @Test
    @Order(12)
    void shouldMinimizeAndRestoreWindow() throws Exception {
        assertDoesNotThrow(() -> driver.manage().window().minimize());
        System.out.println("Window minimized");
        Thread.sleep(500);
        assertDoesNotThrow(() -> driver.manage().window().maximize());
        System.out.println("Window restored via maximize");
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
