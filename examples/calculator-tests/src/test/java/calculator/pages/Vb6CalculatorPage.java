package calculator.pages;

import io.github.winjavadriver.WinBy;
import io.github.winjavadriver.WinJavaDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Page Object for the VB6 Scientific Calculator.
 * Uses WinBy.name for buttons (exact text match) and WinBy.className("VB6Label") for displays.
 *
 * VB6Labels are sorted top-to-bottom by the server:
 *   get(0) = expression display (top, shows "1+2")
 *   get(1) = result display (bottom, shows "3")
 */
public class Vb6CalculatorPage {

    private final WinJavaDriver driver;

    // VB6 button names: operator symbol -> button name (WinBy.name value)
    private static final Map<String, String> BUTTON_NAMES = new HashMap<>();

    static {
        BUTTON_NAMES.put("+", "+");
        BUTTON_NAMES.put("-", "-");
        BUTTON_NAMES.put("*", "*");
        BUTTON_NAMES.put("/", "/");
        BUTTON_NAMES.put("=", "=");
        BUTTON_NAMES.put(".", ".");
        BUTTON_NAMES.put("C", "C");
        BUTTON_NAMES.put("CE", "CE");
        BUTTON_NAMES.put("backspace", "<-");

        // Advanced functions
        BUTTON_NAMES.put("sqrt", "sqrt");
        BUTTON_NAMES.put("sqr", "x^2");
        BUTTON_NAMES.put("1/x", "1/x");
        BUTTON_NAMES.put("negate", "+/-");
    }

    private static final Pattern UNARY_PATTERN = Pattern.compile("^(sqrt|sqr|1/x|negate)\\((.+)\\)$");

    private String lastResult;

    public Vb6CalculatorPage(WinJavaDriver driver) {
        this.driver = driver;
    }

    /**
     * Wait for calculator to be ready.
     * Waits for the "C" (clear) button to be present — it's a windowed control always available.
     */
    public void waitForReady() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(WinBy.name("C")));
    }

    /**
     * Click a digit button (0-9) by its name.
     */
    public void clickDigit(char digit) {
        driver.findElement(WinBy.name(String.valueOf(digit))).click();
    }

    /**
     * Click an operator or function button by symbol.
     */
    public void clickButton(String symbol) {
        String name = BUTTON_NAMES.getOrDefault(symbol, symbol);
        driver.findElement(WinBy.name(name)).click();
    }

    /**
     * Enter a multi-digit number (including decimals).
     */
    public void enterNumber(String number) {
        for (char c : number.toCharArray()) {
            if (c == '.') {
                clickButton(".");
            } else {
                clickDigit(c);
            }
        }
    }

    /**
     * Clear the calculator (press C).
     */
    public void clear() {
        clickButton("C");
    }

    /**
     * Get the current result from the VB6Label display.
     * Labels are sorted top-to-bottom by the server, so:
     *   get(0) = expression display (top), get(last) = result display (bottom)
     */
    public String getResult() {
        List<WebElement> labels = driver.findElements(WinBy.className("VB6Label"));
        if (!labels.isEmpty()) {
            lastResult = labels.get(labels.size() - 1).getText();
        } else {
            lastResult = "";
        }
        return lastResult;
    }

    /**
     * Get the last result obtained (cached, no UI call).
     */
    public String getLastResult() {
        return lastResult;
    }

    /**
     * Compute an expression.
     * Supports:
     *   - Arithmetic: "1 + 2", "10 + 20 + 30"
     *   - Unary: "sqrt(144)", "sqr(5)", "1/x(4)", "negate(15)"
     */
    public void compute(String expression) {
        clear();
        expression = expression.trim();

        // Unary function: func(number)
        Matcher unary = UNARY_PATTERN.matcher(expression);
        if (unary.matches()) {
            String func = unary.group(1);
            String arg = unary.group(2);
            enterNumber(arg);
            clickButton(func);
            return;
        }

        // Arithmetic: "1 + 2" or "10 + 20 + 30"
        String[] tokens = expression.split("\\s+");
        enterNumber(tokens[0]);
        for (int i = 1; i < tokens.length - 1; i += 2) {
            clickButton(tokens[i]);
            enterNumber(tokens[i + 1]);
        }
        clickButton("=");
    }

    /**
     * Close the calculator by clicking the X button.
     */
    public void close() {
        try {
            driver.findElement(WinBy.name("Close")).click();
        } catch (Exception e) {
            try {
                driver.close();
            } catch (Exception ignored) {
            }
        }
    }
}
