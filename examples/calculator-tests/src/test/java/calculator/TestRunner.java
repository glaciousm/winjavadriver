package calculator;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

/**
 * Cucumber test runner for Calculator automation tests.
 *
 * Run all:    mvn test
 * Modern:     mvn test -Dcucumber.filter.tags="@Modern"
 * VB6:        mvn test -Dcucumber.filter.tags="@VB6"
 * Combo:      mvn test -Dcucumber.filter.tags="@Combo"
 *
 * From IntelliJ: right-click this class, or right-click any .feature file / scenario.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/calculator.feature")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-reports/cucumber.html, json:target/cucumber-reports/cucumber.json")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "calculator.steps")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @Ignore")
@ConfigurationParameter(key = PLUGIN_PUBLISH_QUIET_PROPERTY_NAME, value = "true")
public class TestRunner {
}
