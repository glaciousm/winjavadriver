package io.github.winjavadriver.integration;

import io.github.winjavadriver.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for element interaction (click, sendKeys, clear).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InteractionIT {

    private static WinJavaDriverService service;
    private WinJavaDriver driver;

    @BeforeAll
    static void startService() throws IOException {
        File executable = findExecutable();

        service = new WinJavaDriverService.Builder()
                .usingDriverExecutable(executable)
                .usingPort(9519)
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

    @BeforeEach
    void launchApp() {
        WinJavaOptions options = new WinJavaOptions()
                .setApp("notepad.exe")
                .setWaitForAppLaunch(10);

        driver = new WinJavaDriver(service.getUrl(), options);
    }

    @AfterEach
    void closeApp() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should type text in Notepad")
    void shouldTypeTextInNotepad() throws InterruptedException {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));

        String testText = "Hello from WinJavaDriver!";
        textArea.sendKeys(testText);
        Thread.sleep(500);

        String actualText = textArea.getText();
        System.out.println("Typed text: '" + actualText + "'");
        assertThat(actualText).contains("Hello");
    }

    @Test
    @Order(2)
    @DisplayName("Should clear text in Notepad")
    void shouldClearTextInNotepad() throws InterruptedException {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));

        textArea.sendKeys("Text to be cleared");
        Thread.sleep(300);
        assertThat(textArea.getText()).isNotEmpty();

        textArea.clear();
        Thread.sleep(300);

        String afterClear = textArea.getText();
        System.out.println("After clear: '" + afterClear + "'");
        assertThat(afterClear).isEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("Should click on element")
    void shouldClickOnElement() throws InterruptedException {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));

        textArea.click();
        Thread.sleep(200);

        textArea.sendKeys("Clicked and typed!");
        Thread.sleep(300);

        String text = textArea.getText();
        System.out.println("After click and type: '" + text + "'");
        assertThat(text).contains("Clicked");
    }

    @Test
    @Order(4)
    @DisplayName("Should type and clear multiple times")
    void shouldTypeAndClearMultipleTimes() throws InterruptedException {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));

        for (int i = 1; i <= 3; i++) {
            String message = "Iteration " + i;
            textArea.sendKeys(message);
            Thread.sleep(200);
            assertThat(textArea.getText()).contains(message);
            textArea.clear();
            Thread.sleep(200);
            assertThat(textArea.getText()).isEmpty();
        }

        textArea.sendKeys("Final text after iterations");
        Thread.sleep(200);
        System.out.println("Final: " + textArea.getText());
    }

    @Test
    @Order(5)
    @DisplayName("Should handle special characters")
    void shouldHandleSpecialCharacters() throws InterruptedException {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));

        String specialText = "Special: @#$%&*()!";
        textArea.sendKeys(specialText);
        Thread.sleep(300);

        String actual = textArea.getText();
        System.out.println("Special characters: '" + actual + "'");
        assertThat(actual).isNotEmpty();
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
