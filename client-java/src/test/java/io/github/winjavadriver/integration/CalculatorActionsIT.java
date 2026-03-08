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
 * Integration tests for the W3C Actions API using the VB6 Calculator test app.
 * Tests double-click, right-click, and keyboard actions on a legacy Win32 application.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CalculatorActionsIT {

    private static WinJavaDriverService service;
    private WinJavaDriver driver;

    @BeforeAll
    static void startService() throws IOException {
        File executable = findExecutable();
        service = new WinJavaDriverService.Builder()
                .usingDriverExecutable(executable)
                .usingPort(9521)
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
        Path calcPath = Path.of(System.getProperty("user.dir"))
                .resolve("../test-apps/Calculator.exe").toAbsolutePath().normalize();

        WinJavaOptions options = new WinJavaOptions()
                .setApp(calcPath.toString())
                .setWaitForAppLaunch(10);
        driver = new WinJavaDriver(service.getUrl(), options);
    }

    @AfterEach
    void closeApp() {
        if (driver != null) driver.quit();
    }

    @Test
    @Order(1)
    @DisplayName("Should double-click a VB6 calculator button")
    void shouldDoubleClickButton() throws InterruptedException {
        WebElement btnC = driver.findElement(WinBy.name("C"));
        btnC.click();
        Thread.sleep(200);

        WebElement btn5 = driver.findElement(WinBy.name("5"));
        new Actions(driver)
                .doubleClick(btn5)
                .perform();
        Thread.sleep(300);

        // Double-clicking "5" should press it twice, entering "55"
        WebElement display = driver.findElement(WinBy.accessibilityId("lblDisplay"));
        String displayText = display.getText();
        assertThat(displayText).as("Display should show 55 after double-clicking 5").contains("55");
    }

    @Test
    @Order(2)
    @DisplayName("Should right-click on VB6 calculator button")
    void shouldRightClickCalculator() throws InterruptedException {
        WebElement btn = driver.findElement(WinBy.name("5"));

        new Actions(driver)
                .contextClick(btn)
                .perform();
        Thread.sleep(500);

        // Dismiss any context menu with Escape
        new Actions(driver)
                .sendKeys(Keys.ESCAPE)
                .perform();
        Thread.sleep(200);
        System.out.println("Right-click on VB6 button performed and dismissed");
    }

    @Test
    @Order(3)
    @DisplayName("Should hover over VB6 calculator button")
    void shouldHoverOverButton() throws InterruptedException {
        WebElement btn = driver.findElement(WinBy.name("5"));

        new Actions(driver)
                .moveToElement(btn)
                .perform();
        Thread.sleep(300);

        // Verify the hover didn't throw — element should still be displayed
        assertThat(btn.isDisplayed()).isTrue();
        System.out.println("Hover over VB6 button performed successfully");
    }

    private static File findExecutable() {
        Path basePath = Path.of(System.getProperty("user.dir"));
        File exePath = basePath.resolve(
                "../server/WinJavaDriver/bin/Release/net8.0-windows/win-x64/publish/winjavadriver.exe"
        ).toFile();
        return exePath.exists() ? exePath : new File("winjavadriver.exe");
    }
}
