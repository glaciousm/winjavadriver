# Calculator Automation Tests

BDD automation tests for **Windows 11 Calculator** (UWP) and **VB6 Scientific Calculator** using WinJavaDriver and Cucumber.

## Prerequisites

- Windows 10/11
- Java 21+
- Maven 3.8+
- WinJavaDriver server built (see parent project)
- VB6 Calculator app at `../../test-apps/Calculator.exe`

## Running Tests

### From Maven
```bash
# Run all 3 scenarios
mvn test

# Run only Modern Calculator (Windows 11)
mvn test -Dcucumber.filter.tags="@Modern"

# Run only VB6 Calculator
mvn test -Dcucumber.filter.tags="@VB6"

# Run cross-calculator comparison
mvn test -Dcucumber.filter.tags="@Combo"
```

### From IntelliJ
- **Right-click `TestRunner.java`** → Run (runs all scenarios)
- **Right-click `calculator.feature`** → Run (runs all scenarios)
- **Click the green arrow** next to any scenario → Run/Debug that scenario only

## Test Scenarios

### Modern Calculator (@Modern @Smoke)
Tests the Windows 11 Calculator with:
- Basic arithmetic: `5+3=8`, `100-37=63`, `12*9=108`, `144/12=12`
- Advanced functions: `sqrt(81)=9`, `sqr(7)=49`, `1/x(8)=0.125`, `negate(15)=-15`
- Multi-step: `25+75-50=50`

### VB6 Calculator (@VB6)
Tests the VB6 Scientific Calculator with:
- Basic arithmetic: `1+2=3`, `999-123=876`, `15*8=120`, `200/4=50`
- Advanced functions: `sqrt(144)=12`, `sqr(5)=25`, `1/x(4)=0.25`
- Large numbers: `12345+67890=80235`

### Cross-Calculator Comparison (@Combo)
Runs the same operations on both calculators and verifies identical results:
- `7+8=15`, `50-17=33`, `6*9=54`, `100/8=12.5`, `sqrt(49)=7`

## Architecture

### Page Objects
- **`ModernCalculatorPage`** — Windows 11 Calculator via `WinBy.accessibilityId()` locators
- **`Vb6CalculatorPage`** — VB6 Calculator via `WinBy.name()` for buttons and `WinBy.className("VB6Label")` for display labels

### VB6 Label Reading
VB6 Label controls are **windowless** (no HWND). WinJavaDriver reads their runtime captions by:
1. Scanning the VB6 process heap for the control block array (232-byte stride)
2. Finding individual control objects via "VB.Label" type-string matching
3. Reading the runtime Caption BSTR at offset 136 in each control object
4. Matching captions to positions using twips coordinates

### Step Definitions
- **`ModernCalculatorSteps`** — Steps for Modern Calculator operations
- **`Vb6CalculatorSteps`** — Steps for VB6 Calculator operations
- **`ComboCalculatorSteps`** — Cross-calculator comparison assertions
- **`Hooks`** — Lifecycle management using SeleniumHQ pattern (`new WinJavaDriver(options)` — auto-discovers exe, auto-manages server)

## Project Structure

```
calculator-tests/
├── pom.xml
├── README.md
└── src/test/
    ├── java/calculator/
    │   ├── TestRunner.java
    │   ├── pages/
    │   │   ├── ModernCalculatorPage.java
    │   │   └── Vb6CalculatorPage.java
    │   └── steps/
    │       ├── ComboCalculatorSteps.java
    │       ├── Hooks.java
    │       ├── ModernCalculatorSteps.java
    │       └── Vb6CalculatorSteps.java
    └── resources/
        └── features/
            └── calculator.feature
```

## Reports

After running tests, HTML and JSON reports are generated at:
- `target/cucumber-reports/cucumber.html`
- `target/cucumber-reports/cucumber.json`

## Troubleshooting

### Port conflict
Each driver auto-selects a free port via Selenium's `PortProber`. If stale processes remain:
```bash
taskkill /F /IM winjavadriver.exe
```

### winjavadriver.exe not found
The driver auto-discovers the exe via: system property `webdriver.winjavadriver.driver` → PATH → known locations (`../../server/.../publish/`). Set the system property or add the exe to your PATH.

### VB6 Calculator not found
The VB6 path is resolved automatically from the project's class location. Ensure `Calculator.exe` exists at `../../test-apps/Calculator.exe` relative to this project. Alternatively, set the system property `vb6calculator.exe` to override.

### VB6 labels show design-time text instead of runtime values
The WinJavaDriver server must be the latest build with the `ScanVb6HeapForLabelCaptions` method. Rebuild:
```bash
cd ../../server/WinJavaDriver
dotnet publish -c Release -r win-x64 --self-contained
```
