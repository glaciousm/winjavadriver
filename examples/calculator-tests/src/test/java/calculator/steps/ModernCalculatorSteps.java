package calculator.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import calculator.pages.ModernCalculatorPage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for Modern Windows Calculator scenarios.
 */
public class ModernCalculatorSteps {

    private ModernCalculatorPage page;

    @Given("the Modern Calculator is open")
    public void theModernCalculatorIsOpen() {
        page = Hooks.getModernPage();
        assertThat(page).as("Modern Calculator page should be initialized").isNotNull();
    }

    @When("I compute {string} on the Modern Calculator")
    public void iComputeOnTheModernCalculator(String expression) {
        page.compute(expression);
    }

    @Then("the Modern Calculator result should be {string}")
    public void theModernCalculatorResultShouldBe(String expected) {
        String result = page.getResult();
        assertThat(result)
                .as("Modern Calculator result for last operation")
                .isEqualTo(expected);
    }

    @And("I close the Modern Calculator")
    public void iCloseTheModernCalculator() {
        page.close();
    }
}
