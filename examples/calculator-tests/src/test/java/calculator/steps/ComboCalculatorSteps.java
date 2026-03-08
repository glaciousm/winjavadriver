package calculator.steps;

import io.cucumber.java.en.Then;
import calculator.pages.ModernCalculatorPage;
import calculator.pages.Vb6CalculatorPage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the Combo scenario where both calculators
 * are compared against each other for identical results.
 */
public class ComboCalculatorSteps {

    @Then("both calculators should show {string}")
    public void bothCalculatorsShouldShow(String expected) {
        ModernCalculatorPage modern = Hooks.getModernPage();
        Vb6CalculatorPage vb6 = Hooks.getVb6Page();

        String modernResult = modern.getResult();
        String vb6Result = vb6.getResult();

        assertThat(modernResult)
                .as("Modern Calculator result")
                .isEqualTo(expected);

        assertThat(vb6Result)
                .as("VB6 Calculator result")
                .isEqualTo(expected);

        assertThat(modernResult)
                .as("Both calculators should produce identical results")
                .isEqualTo(vb6Result);
    }
}
