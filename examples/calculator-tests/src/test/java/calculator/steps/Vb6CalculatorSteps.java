package calculator.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import calculator.pages.Vb6CalculatorPage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for VB6 Calculator scenarios.
 */
public class Vb6CalculatorSteps {

    private Vb6CalculatorPage page;

    @Given("the VB6 Calculator is open")
    public void theVb6CalculatorIsOpen() {
        page = Hooks.getVb6Page();
        assertThat(page).as("VB6 Calculator page should be initialized").isNotNull();
    }

    @When("I compute {string} on the VB6 Calculator")
    public void iComputeOnTheVb6Calculator(String expression) {
        page.compute(expression);
    }

    @Then("the VB6 Calculator display should show {string}")
    public void theVb6CalculatorDisplayShouldShow(String expected) {
        String result = page.getResult();
        assertThat(result)
                .as("VB6 Calculator display for last operation")
                .isEqualTo(expected);
    }

    @And("I close the VB6 Calculator")
    public void iCloseTheVb6Calculator() {
        page.close();
    }
}
