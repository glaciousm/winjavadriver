package calculator.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.github.winjavadriver.WinJavaDriver;
import io.github.winjavadriver.WinJavaOptions;
import org.openqa.selenium.OutputType;
import calculator.pages.ModernCalculatorPage;
import calculator.pages.Vb6CalculatorPage;

import java.io.File;
import java.net.URL;

/**
 * Cucumber hooks for Calculator test lifecycle.
 * Each driver auto-discovers winjavadriver.exe and manages its own server —
 * identical to how ChromeDriver works in SeleniumHQ.
 *
 * <p>For Grid execution, set system property:
 * {@code -Dwinjavadriver.grid.url=http://hub:4444}
 */
public class Hooks {

    private static final String DEFAULT_VB6_PATH = resolveVb6Path();
    private static final String GRID_URL = System.getProperty("winjavadriver.grid.url");

    // Thread-local state for parallel execution
    private static final ThreadLocal<WinJavaDriver> modernDriverHolder = new ThreadLocal<>();
    private static final ThreadLocal<WinJavaDriver> vb6DriverHolder = new ThreadLocal<>();
    private static final ThreadLocal<ModernCalculatorPage> modernPageHolder = new ThreadLocal<>();
    private static final ThreadLocal<Vb6CalculatorPage> vb6PageHolder = new ThreadLocal<>();

    @Before(value = "@Modern or @Combo", order = 1)
    public void setUpModern(Scenario scenario) {
        System.out.println("[Modern] Launching Windows Calculator...");

        WinJavaOptions options = new WinJavaOptions()
                .setApp("calc.exe")
                .setWaitForAppLaunch(10);

        WinJavaDriver driver = createDriver(options);
        modernDriverHolder.set(driver);

        ModernCalculatorPage page = new ModernCalculatorPage(driver);
        page.waitForReady();
        modernPageHolder.set(page);

        System.out.println("[Modern] Calculator ready" + (GRID_URL != null ? " (via Grid)" : ""));
    }

    @Before(value = "@VB6 or @Combo", order = 2)
    public void setUpVb6(Scenario scenario) {
        System.out.println("[VB6] Launching VB6 Calculator...");

        String vb6Path = System.getProperty("vb6calculator.exe", DEFAULT_VB6_PATH);

        WinJavaOptions options = new WinJavaOptions()
                .setApp(vb6Path)
                .setWaitForAppLaunch(10);

        WinJavaDriver driver = createDriver(options);
        vb6DriverHolder.set(driver);

        Vb6CalculatorPage page = new Vb6CalculatorPage(driver);
        page.waitForReady();
        vb6PageHolder.set(page);

        System.out.println("[VB6] Calculator ready");
    }

    @After
    public void tearDown(Scenario scenario) {
        // Screenshot on failure for Modern driver
        WinJavaDriver modernDriver = modernDriverHolder.get();
        if (modernDriver != null) {
            if (scenario.isFailed()) {
                takeFailureScreenshot(modernDriver, scenario.getName(), "modern");
            }
            try {
                modernDriver.quit();
            } catch (Exception e) {
                System.err.println("[Modern] Failed to quit: " + e.getMessage());
            }
        }

        // Screenshot on failure for VB6 driver
        WinJavaDriver vb6Driver = vb6DriverHolder.get();
        if (vb6Driver != null) {
            if (scenario.isFailed()) {
                takeFailureScreenshot(vb6Driver, scenario.getName(), "vb6");
            }
            try {
                vb6Driver.quit();
            } catch (Exception e) {
                System.err.println("[VB6] Failed to quit: " + e.getMessage());
            }
        }

        // Clean up ThreadLocals
        modernDriverHolder.remove();
        vb6DriverHolder.remove();
        modernPageHolder.remove();
        vb6PageHolder.remove();
    }

    private void takeFailureScreenshot(WinJavaDriver driver, String scenarioName, String prefix) {
        try {
            File dir = new File("target/screenshots/failures");
            dir.mkdirs();

            String safeName = scenarioName
                    .replaceAll("[^a-zA-Z0-9]", "_")
                    .substring(0, Math.min(50, scenarioName.length()));
            File screenshotFile = driver.getScreenshotAs(OutputType.FILE);

            File dest = new File(dir, prefix + "_" + safeName + "_failure.png");
            screenshotFile.renameTo(dest);
            System.out.println("Failure screenshot saved: " + dest.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }

    public static ModernCalculatorPage getModernPage() {
        return modernPageHolder.get();
    }

    public static Vb6CalculatorPage getVb6Page() {
        return vb6PageHolder.get();
    }

    /**
     * Creates a WinJavaDriver — either local or via Selenium Grid.
     * Set {@code -Dwinjavadriver.grid.url=http://hub:4444} to route through Grid.
     */
    private static WinJavaDriver createDriver(WinJavaOptions options) {
        try {
            if (GRID_URL != null) {
                return new WinJavaDriver(new URL(GRID_URL), options);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Grid at " + GRID_URL, e);
        }
        return new WinJavaDriver(options);
    }

    /**
     * Resolves the VB6 Calculator path relative to this project's source tree,
     * so it works from both Maven (cwd=project root) and IntelliJ (cwd may vary).
     */
    private static String resolveVb6Path() {
        // Walk up from this class's location to find the project root
        java.net.URL classUrl = Hooks.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            java.io.File classDir = new java.io.File(classUrl.toURI());
            // classDir is typically target/test-classes — go up 2 levels to project root
            java.io.File projectRoot = classDir.getParentFile().getParentFile();
            java.io.File vb6Exe = new java.io.File(projectRoot, "../../test-apps/Calculator.exe");
            if (vb6Exe.exists()) {
                return vb6Exe.getAbsolutePath();
            }
        } catch (Exception ignored) {
        }
        // Fallback to relative path (works when cwd is the project directory)
        return "../../test-apps/Calculator.exe";
    }
}
