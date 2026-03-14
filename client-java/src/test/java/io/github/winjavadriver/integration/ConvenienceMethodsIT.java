package io.github.winjavadriver.integration;

import io.github.winjavadriver.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for WinJavaDriver 1.1.0 convenience methods.
 * Uses charmap.exe (Character Map), a classic Win32 app available on all Windows versions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConvenienceMethodsIT {

    private static WinJavaDriverService service;
    private WinJavaDriver driver;

    @BeforeAll
    static void startService() throws IOException {
        File executable = findExecutable();
        service = new WinJavaDriverService.Builder()
                .usingDriverExecutable(executable)
                .usingPort(9523)
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

    // =============================================
    // Wait Helpers
    // =============================================

    @Test
    @Order(1)
    @DisplayName("waitForElement should find a known charmap element")
    void waitForElementShouldFindElement() {
        WebElement el = driver.waitForElement(WinBy.name("Character Map"));
        assertThat(el).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("waitForElement with custom timeout should work")
    void waitForElementWithTimeoutShouldWork() {
        WebElement el = driver.waitForElement(WinBy.name("Character Map"), Duration.ofSeconds(5));
        assertThat(el).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("waitForClickable should find a clickable element")
    void waitForClickableShouldFindElement() {
        // "Select" button is always enabled in charmap
        WebElement el = driver.waitForClickable(WinBy.name("Select"), Duration.ofSeconds(5));
        assertThat(el).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("waitForWindowCount should return current windows")
    void waitForWindowCountShouldReturnHandles() {
        Set<String> handles = driver.waitForWindowCount(1);
        assertThat(handles).hasSize(1);
    }

    // =============================================
    // sendKeys
    // =============================================

    @Test
    @Order(5)
    @DisplayName("sendKeys should send keyboard input without element reference")
    void sendKeysShouldWork() {
        // Tab to navigate — should not throw
        driver.sendKeys(org.openqa.selenium.Keys.TAB);
    }

    // =============================================
    // listAllWindows
    // =============================================

    @Test
    @Order(6)
    @DisplayName("listAllWindows should return visible windows on the system")
    void listAllWindowsShouldWork() {
        List<Map<String, Object>> windows = driver.listAllWindows();
        assertThat(windows).isNotEmpty();

        // Should contain charmap
        boolean found = windows.stream()
                .anyMatch(w -> w.get("title") != null && w.get("title").toString().contains("Character Map"));
        assertThat(found).as("Should find Character Map in window list").isTrue();

        // Each window should have handle, title, className, processId
        Map<String, Object> firstWindow = windows.get(0);
        assertThat(firstWindow).containsKeys("handle", "title", "className", "processId");
    }

    // =============================================
    // switchToWindowByTitle
    // =============================================

    @Test
    @Order(7)
    @DisplayName("switchToWindowByTitle should find window by title fragment")
    void switchToWindowByTitleShouldWork() {
        WinJavaDriver returned = driver.switchToWindowByTitle("Character Map");
        assertThat(returned).isSameAs(driver);
    }

    @Test
    @Order(8)
    @DisplayName("switchToWindowByTitle should throw for non-existent title")
    void switchToWindowByTitleShouldThrowForMissing() {
        assertThatThrownBy(() -> driver.switchToWindowByTitle("NonExistentWindowTitle12345"))
                .isInstanceOf(NoSuchWindowException.class)
                .hasMessageContaining("NonExistentWindowTitle12345");
    }

    // =============================================
    // findElementByIndex
    // =============================================

    @Test
    @Order(9)
    @DisplayName("findElementByIndex should return element at given index")
    void findElementByIndexShouldWork() {
        // Find buttons in charmap — there should be multiple
        WebElement el = driver.findElementByIndex(WinBy.className("Button"), 0);
        assertThat(el).isNotNull();
    }

    @Test
    @Order(10)
    @DisplayName("findElementByIndex should throw for out-of-range index")
    void findElementByIndexShouldThrowForBadIndex() {
        assertThatThrownBy(() -> driver.findElementByIndex(WinBy.className("Button"), 999))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("999");
    }

    // =============================================
    // findChildren
    // =============================================

    @Test
    @Order(11)
    @DisplayName("findChildren should return direct children of an element")
    void findChildrenShouldWork() {
        // The charmap window should have child controls (buttons, dropdowns, etc.)
        WebElement window = driver.findElement(WinBy.name("Character Map"));
        List<WebElement> children = driver.findChildren(window);
        assertThat(children).isNotEmpty();
    }

    @Test
    @Order(12)
    @DisplayName("findChildren with index should return Nth child")
    void findChildrenWithIndexShouldWork() {
        WebElement window = driver.findElement(WinBy.name("Character Map"));
        WebElement child = driver.findChildren(window, 0);
        assertThat(child).isNotNull();
    }

    @Test
    @Order(13)
    @DisplayName("findChildren with bad index should throw")
    void findChildrenWithBadIndexShouldThrow() {
        WebElement window = driver.findElement(WinBy.name("Character Map"));
        assertThatThrownBy(() -> driver.findChildren(window, 999))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    // =============================================
    // findElementByPosition
    // =============================================

    @Test
    @Order(14)
    @DisplayName("findElementByPosition should find element by row/col")
    void findElementByPositionShouldWork() {
        // Charmap has buttons — find the first one by position
        WebElement el = driver.findElementByPosition(WinBy.className("Button"), 0, 0);
        assertThat(el).isNotNull();
    }

    @Test
    @Order(15)
    @DisplayName("findElementByPosition with bad row should throw")
    void findElementByPositionBadRowShouldThrow() {
        assertThatThrownBy(() -> driver.findElementByPosition(WinBy.className("Button"), 999, 0))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("row");
    }

    @Test
    @Order(16)
    @DisplayName("findElementByPosition with custom tolerance should work")
    void findElementByPositionWithToleranceShouldWork() {
        WebElement el = driver.findElementByPosition(WinBy.className("Button"), 0, 0, 30);
        assertThat(el).isNotNull();
    }

    // =============================================
    // retry
    // =============================================

    @Test
    @Order(17)
    @DisplayName("retry should succeed on first try when action works")
    void retryShouldSucceedOnFirstTry() {
        AtomicInteger counter = new AtomicInteger(0);
        driver.retry(() -> counter.incrementAndGet(), 3, Duration.ofMillis(100));
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @Order(18)
    @DisplayName("retry should succeed on Nth try")
    void retryShouldSucceedOnNthTry() {
        AtomicInteger counter = new AtomicInteger(0);
        driver.retry(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new RuntimeException("not yet");
            }
        }, 5, Duration.ofMillis(50));
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    @Order(19)
    @DisplayName("retry should throw after all attempts fail")
    void retryShouldThrowAfterAllFail() {
        assertThatThrownBy(() ->
                driver.retry(() -> { throw new RuntimeException("always fails"); }, 3, Duration.ofMillis(50))
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("3 attempts");
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
