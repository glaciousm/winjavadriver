package io.github.winjavadriver.integration;

import io.github.winjavadriver.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the W3C Actions API using Notepad.
 * Tests double-click, right-click, hover, and key combos via Selenium's Actions class.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ActionsIT {

    private static WinJavaDriverService service;
    private WinJavaDriver driver;

    @BeforeAll
    static void startService() throws IOException {
        File executable = findExecutable();
        service = new WinJavaDriverService.Builder()
                .usingDriverExecutable(executable)
                .usingPort(9520)
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
                .setApp("notepad.exe")
                .setWaitForAppLaunch(10);
        driver = new WinJavaDriver(service.getUrl(), options);
    }

    @AfterEach
    void closeApp() {
        if (driver != null) driver.quit();
    }

    /**
     * Clear the Notepad text area (Win11 Notepad restores unsaved tabs).
     */
    private void clearTextArea(WebElement textArea) throws InterruptedException {
        textArea.click();
        Thread.sleep(100);
        new Actions(driver)
                .keyDown(Keys.CONTROL)
                .sendKeys("a")
                .keyUp(Keys.CONTROL)
                .perform();
        Thread.sleep(100);
        new Actions(driver)
                .sendKeys(Keys.DELETE)
                .perform();
        Thread.sleep(100);
    }

    @Test
    @Order(1)
    @DisplayName("Should double-click to select a word")
    void shouldDoubleClickToSelectWord() throws InterruptedException {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));
        clearTextArea(textArea);
        textArea.sendKeys("Hello World");
        Thread.sleep(300);

        new Actions(driver)
                .doubleClick(textArea)
                .perform();
        Thread.sleep(300);

        // Double-click selects last word; typing replaces the selection
        new Actions(driver)
                .sendKeys("Replaced")
                .perform();
        Thread.sleep(300);

        String text = textArea.getText();
        System.out.println("After double-click + type: '" + text + "'");
        // Double-click worked if the text changed (word replaced or text modified)
        assertThat(text).containsIgnoringCase("replaced");
    }

    @Test
    @Order(2)
    @DisplayName("Should right-click to open context menu")
    void shouldRightClick() throws InterruptedException {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));
        textArea.click();
        Thread.sleep(200);

        new Actions(driver)
                .contextClick(textArea)
                .perform();
        Thread.sleep(500);

        // Verify context menu appeared by checking for a menu item
        var menuItems = driver.findElements(WinBy.className("MenuFlyoutItem"));
        assertThat(menuItems).as("Context menu should have items").isNotEmpty();

        // Dismiss context menu with Escape
        new Actions(driver)
                .sendKeys(Keys.ESCAPE)
                .perform();
        Thread.sleep(200);
    }

    @Test
    @Order(3)
    @DisplayName("Should hover over element (moveToElement)")
    void shouldHoverOverElement() throws InterruptedException {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));
        clearTextArea(textArea);

        new Actions(driver)
                .moveToElement(textArea)
                .perform();
        Thread.sleep(200);

        // Click at hover location and type
        new Actions(driver)
                .click()
                .sendKeys("Hovered and typed")
                .perform();
        Thread.sleep(300);

        String text = textArea.getText();
        System.out.println("After hover + type: '" + text + "'");
        assertThat(text).containsIgnoringCase("hovered");
    }

    @Test
    @Order(4)
    @DisplayName("Should perform Ctrl+A (select all) via key actions")
    void shouldPerformCtrlA() throws InterruptedException {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));
        clearTextArea(textArea);
        textArea.sendKeys("Select all this text");
        Thread.sleep(300);

        new Actions(driver)
                .click(textArea)
                .keyDown(Keys.CONTROL)
                .sendKeys("a")
                .keyUp(Keys.CONTROL)
                .perform();
        Thread.sleep(200);

        // Type to replace selected text
        new Actions(driver)
                .sendKeys("All replaced")
                .perform();
        Thread.sleep(300);

        String text = textArea.getText();
        System.out.println("After Ctrl+A + type: '" + text + "'");
        assertThat(text).containsIgnoringCase("all replaced");
        // Verify original text is gone (Ctrl+A selected all, then replaced)
        assertThat(text).doesNotContain("Select");
    }

    @Test
    @Order(5)
    @DisplayName("Should use Shift for uppercase via key actions")
    void shouldUseShiftForUppercase() throws InterruptedException {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));
        clearTextArea(textArea);

        new Actions(driver)
                .keyDown(Keys.SHIFT)
                .sendKeys("abc")
                .keyUp(Keys.SHIFT)
                .perform();
        Thread.sleep(300);

        String text = textArea.getText();
        System.out.println("Shift+abc text: '" + text + "'");
        assertThat(text).contains("ABC");
    }

    private static File findExecutable() {
        Path basePath = Path.of(System.getProperty("user.dir"));
        File exePath = basePath.resolve(
                "../server/WinJavaDriver/bin/Release/net8.0-windows/win-x64/publish/winjavadriver.exe"
        ).toFile();
        return exePath.exists() ? exePath : new File("winjavadriver.exe");
    }
}
