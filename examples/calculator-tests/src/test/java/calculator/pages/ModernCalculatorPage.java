package calculator.pages;

import io.github.winjavadriver.WinBy;
import io.github.winjavadriver.WinJavaDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page Object for the Windows 11 Modern Calculator (UWP).
 * Uses AccessibilityId locators for reliable element discovery.
 */
public class ModernCalculatorPage {

    private final WinJavaDriver driver;

    // Digit AccessibilityIds: num0Button .. num9Button
    private static final Map<Character, String> DIGIT_IDS = new HashMap<>();

    // Operator/function AccessibilityIds
    private static final Map<String, String> BUTTON_IDS = new HashMap<>();

    static {
        // Digits
        DIGIT_IDS.put('0', "num0Button");
        DIGIT_IDS.put('1', "num1Button");
        DIGIT_IDS.put('2', "num2Button");
        DIGIT_IDS.put('3', "num3Button");
        DIGIT_IDS.put('4', "num4Button");
        DIGIT_IDS.put('5', "num5Button");
        DIGIT_IDS.put('6', "num6Button");
        DIGIT_IDS.put('7', "num7Button");
        DIGIT_IDS.put('8', "num8Button");
        DIGIT_IDS.put('9', "num9Button");

        // Operators
        BUTTON_IDS.put("+", "plusButton");
        BUTTON_IDS.put("-", "minusButton");
        BUTTON_IDS.put("*", "multiplyButton");
        BUTTON_IDS.put("/", "divideButton");
        BUTTON_IDS.put("=", "equalButton");
        BUTTON_IDS.put(".", "decimalSeparatorButton");
        BUTTON_IDS.put("C", "clearButton");
        BUTTON_IDS.put("CE", "clearEntryButton");
        BUTTON_IDS.put("backspace", "backSpaceButton");

        // Advanced functions
        BUTTON_IDS.put("sqrt", "squareRootButton");
        BUTTON_IDS.put("sqr", "xpower2Button");
        BUTTON_IDS.put("1/x", "invertButton");
        BUTTON_IDS.put("negate", "negateButton");
    }

    private static final By RESULT_DISPLAY = WinBy.accessibilityId("CalculatorResults");
    private static final Pattern UNARY_PATTERN = Pattern.compile("^(sqrt|sqr|1/x|negate)\\((.+)\\)$");

    private String lastResult;

    public ModernCalculatorPage(WinJavaDriver driver) {
        this.driver = driver;
    }

    /**
     * Wait for calculator to be ready (result display present).
     */
    public void waitForReady() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(RESULT_DISPLAY));
    }

    /**
     * Click a digit button (0-9).
     */
    public void clickDigit(char digit) {
        String id = DIGIT_IDS.get(digit);
        if (id == null) {
            throw new IllegalArgumentException("Invalid digit: " + digit);
        }
        driver.findElement(WinBy.accessibilityId(id)).click();
    }

    /**
     * Click an operator or function button by symbol.
     */
    public void clickButton(String symbol) {
        String id = BUTTON_IDS.get(symbol);
        if (id == null) {
            throw new IllegalArgumentException("Unknown button: " + symbol);
        }
        driver.findElement(WinBy.accessibilityId(id)).click();
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
     * Get the current result from the display.
     * Strips "Display is " prefix and formatting characters.
     */
    public String getResult() {
        WebElement display = driver.findElement(RESULT_DISPLAY);
        String text = display.getText();
        if (text.startsWith("Display is ")) {
            text = text.substring("Display is ".length());
        }
        // Remove commas and spaces (thousand separators)
        text = text.replaceAll("[,\\s]", "");
        lastResult = text;
        return text;
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
     *   - Arithmetic: "5 + 3", "25 + 75 - 50"
     *   - Unary: "sqrt(81)", "sqr(7)", "1/x(8)", "negate(15)"
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

        // Arithmetic: "5 + 3" or "25 + 75 - 50"
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
            // Fallback: close via W3C command
            try {
                driver.close();
            } catch (Exception ignored) {
            }
        }
    }
}
