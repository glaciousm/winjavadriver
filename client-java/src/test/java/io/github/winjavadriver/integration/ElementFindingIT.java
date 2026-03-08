package io.github.winjavadriver.integration;

import io.github.winjavadriver.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for element finding and property reading.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ElementFindingIT {

    private static WinJavaDriverService service;
    private WinJavaDriver driver;

    @BeforeAll
    static void startService() throws IOException {
        File executable = findExecutable();

        service = new WinJavaDriverService.Builder()
                .usingDriverExecutable(executable)
                .usingPort(9518)
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
    @DisplayName("Should find element by class name")
    void shouldFindElementByClassName() {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));

        assertThat(textArea).isNotNull();
        System.out.println("Found Edit element: " + textArea);
    }

    @Test
    @Order(2)
    @DisplayName("Should get element properties")
    void shouldGetElementProperties() {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));

        boolean enabled = textArea.isEnabled();
        assertThat(enabled).isTrue();
        System.out.println("Element enabled: " + enabled);

        boolean displayed = textArea.isDisplayed();
        assertThat(displayed).isTrue();
        System.out.println("Element displayed: " + displayed);

        String tagName = textArea.getTagName();
        assertThat(tagName).isNotEmpty();
        System.out.println("Element tag name: " + tagName);

        Rectangle rect = textArea.getRect();
        assertThat(rect.getWidth()).isGreaterThan(0);
        assertThat(rect.getHeight()).isGreaterThan(0);
        System.out.println("Element rect: " + rect);
    }

    @Test
    @Order(3)
    @DisplayName("Should get text from element")
    void shouldGetTextFromElement() {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));
        String text = textArea.getText();
        System.out.println("Initial text: '" + text + "'");
    }

    @Test
    @Order(4)
    @DisplayName("Should find multiple elements")
    void shouldFindMultipleElements() {
        List<WebElement> buttons = driver.findElements(By.tagName("Button"));

        System.out.println("Found " + buttons.size() + " buttons");
        for (WebElement button : buttons) {
            System.out.println("  - Button: " + button);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should throw NoSuchElementException for missing element")
    void shouldThrowForMissingElement() {
        assertThatThrownBy(() -> driver.findElement(WinBy.name("NonExistentElement123")))
                .isInstanceOf(org.openqa.selenium.NoSuchElementException.class);
    }

    @Test
    @Order(6)
    @DisplayName("Should return empty list for missing elements")
    void shouldReturnEmptyListForMissingElements() {
        List<WebElement> elements = driver.findElements(WinBy.name("NonExistentElement123"));
        assertThat(elements).isEmpty();
    }

    @Test
    @Order(7)
    @DisplayName("Should find element using explicit wait")
    void shouldFindElementWithExplicitWait() {
        WebElement textArea = new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.presenceOfElementLocated(WinBy.className("RichEditD2DPT")));

        assertThat(textArea).isNotNull();
        System.out.println("Found element with wait: " + textArea);
    }

    @Test
    @Order(8)
    @DisplayName("Should wait for element to be visible (visibilityOfElementLocated)")
    void shouldWaitForElementVisible() {
        WebElement textArea = new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(WinBy.className("RichEditD2DPT")));

        assertThat(textArea).isNotNull();
        assertThat(textArea.isDisplayed()).isTrue();
        assertThat(textArea.isEnabled()).isTrue();
        System.out.println("Element is visible and enabled: " + textArea);
    }

    @Test
    @Order(9)
    @DisplayName("Should get element attribute")
    void shouldGetElementAttribute() {
        WebElement textArea = driver.findElement(WinBy.className("RichEditD2DPT"));

        String className = textArea.getAttribute("className");
        System.out.println("ClassName attribute: " + className);
        assertThat(className).isNotEmpty();
    }

    @Test
    @Order(10)
    @DisplayName("Should use elementToBeClickable (requires isDisplayed)")
    void shouldUseElementToBeClickable() {
        WebElement textArea = new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.elementToBeClickable(WinBy.className("RichEditD2DPT")));

        assertThat(textArea).isNotNull();
        System.out.println("Element is clickable: " + textArea);
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
