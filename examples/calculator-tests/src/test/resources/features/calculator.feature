@Calculator
Feature: Calculator Automation
  Verify arithmetic operations on both Windows 11 Calculator and VB6 Calculator

  @Modern @Smoke
  Scenario: Modern Windows Calculator performs basic and advanced operations
    Given the Modern Calculator is open
    # Basic arithmetic
    When I compute "5 + 3" on the Modern Calculator
    Then the Modern Calculator result should be "8"
    When I compute "100 - 37" on the Modern Calculator
    Then the Modern Calculator result should be "63"
    When I compute "12 * 9" on the Modern Calculator
    Then the Modern Calculator result should be "108"
    When I compute "144 / 12" on the Modern Calculator
    Then the Modern Calculator result should be "12"
    # Advanced operations
    When I compute "sqrt(81)" on the Modern Calculator
    Then the Modern Calculator result should be "9"
    When I compute "sqr(7)" on the Modern Calculator
    Then the Modern Calculator result should be "49"
    When I compute "1/x(8)" on the Modern Calculator
    Then the Modern Calculator result should be "0.125"
    When I compute "negate(15)" on the Modern Calculator
    Then the Modern Calculator result should be "-15"
    # Multi-step
    When I compute "25 + 75 - 50" on the Modern Calculator
    Then the Modern Calculator result should be "50"
    And I close the Modern Calculator

  @VB6
  Scenario: VB6 Calculator performs basic operations with Label verification
    Given the VB6 Calculator is open
    # Basic arithmetic
    When I compute "1 + 2" on the VB6 Calculator
    Then the VB6 Calculator display should show "3"
    When I compute "999 - 123" on the VB6 Calculator
    Then the VB6 Calculator display should show "876"
    When I compute "15 * 8" on the VB6 Calculator
    Then the VB6 Calculator display should show "120"
    When I compute "200 / 4" on the VB6 Calculator
    Then the VB6 Calculator display should show "50"
    # Advanced operations
    When I compute "sqrt(144)" on the VB6 Calculator
    Then the VB6 Calculator display should show "12"
    When I compute "sqr(5)" on the VB6 Calculator
    Then the VB6 Calculator display should show "25"
    When I compute "1/x(4)" on the VB6 Calculator
    Then the VB6 Calculator display should show "0.25"
    # Larger numbers
    When I compute "12345 + 67890" on the VB6 Calculator
    Then the VB6 Calculator display should show "80235"
    And I close the VB6 Calculator

  @Combo
  Scenario: Both calculators produce identical results for the same operations
    Given the Modern Calculator is open
    And the VB6 Calculator is open
    # Same operations, same results
    When I compute "7 + 8" on the Modern Calculator
    And I compute "7 + 8" on the VB6 Calculator
    Then both calculators should show "15"
    When I compute "50 - 17" on the Modern Calculator
    And I compute "50 - 17" on the VB6 Calculator
    Then both calculators should show "33"
    When I compute "6 * 9" on the Modern Calculator
    And I compute "6 * 9" on the VB6 Calculator
    Then both calculators should show "54"
    When I compute "100 / 8" on the Modern Calculator
    And I compute "100 / 8" on the VB6 Calculator
    Then both calculators should show "12.5"
    When I compute "sqrt(49)" on the Modern Calculator
    And I compute "sqrt(49)" on the VB6 Calculator
    Then both calculators should show "7"
    And I close the Modern Calculator
    And I close the VB6 Calculator
