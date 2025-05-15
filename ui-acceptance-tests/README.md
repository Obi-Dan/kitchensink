# Kitchensink UI Acceptance Tests

This module contains UI acceptance tests for the Kitchensink application, built using Playwright and Java.

## Overview

The tests aim to verify the functionality of the web user interface as described in the Business Requirements (specifically section 2.1).

## Technology Stack

- **Java 11**: The programming language used for writing tests.
- **Playwright**: The browser automation framework used for interacting with the UI. Version: `1.44.0` (or as specified in `pom.xml`).
- **JUnit 5**: The testing framework used for structuring and running tests.
- **Maven**: The build automation and dependency management tool.

## Test Structure

Tests are located in `src/test/java/org/jboss/as/quickstarts/kitchensink/ui/`.

- `MemberRegistrationUITest.java`: Contains tests related to member registration, including form submission, validation, and listing of members.

## Video Recording

Each test execution is automatically recorded. Videos are saved to the `ui-acceptance-tests/target/videos/` directory.
- The video files have randomly generated names (e.g., `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx-page-x.webm`).
- The console output during the test run will indicate the exact path to the saved video file for each test.

## Running Tests

Tests are typically run via the Makefile command `make ui-test` or as part of `make test-all`.

To run tests directly using Maven from the `ui-acceptance-tests` directory:

```bash
mvn test
```

**Prerequisites for running tests:**
1. The Kitchensink application must be running and accessible at `http://localhost:8080/kitchensink/`.
2. Maven must be installed.
3. Playwright browsers need to be installed. If they are not, Playwright will attempt to download them on the first run. Alternatively, you can install them manually:
   ```bash
   mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
   ```
   (Run this command from the `ui-acceptance-tests` directory).

## Reasoning and Decisions

- **Playwright**: Chosen for its modern API, cross-browser capabilities, and built-in support for auto-waits, tracing, and video recording, which directly addresses a user requirement.
- **Java**: Aligns with the existing `acceptance-tests` module and the main application's language.
- **JUnit 5**: A standard and widely used testing framework in the Java ecosystem.
- **Video Recording Path**: Videos are saved to `target/videos/` within the module. This is a standard Maven target directory and keeps build artifacts separate from source code.
- **Test Focus**: Initial tests focus on core member registration functionality as per REQ-2.1.5, REQ-2.1.6, REQ-2.1.7, REQ-2.1.8.

## Future Considerations

- **More Comprehensive Tests**: Expand test coverage to include all UI-related business requirements (e.g., REQ-2.1.9, REQ-2.1.10, REQ-2.1.11).
- **Data Management**: Implement more robust pre-test and post-test data setup/cleanup, especially for tests that rely on a specific state (e.g., no members present).
- **Cross-Browser Testing**: Configure Playwright to run tests across different browsers (Chromium, Firefox, WebKit).
- **CI Integration**: Integrate these tests into a Continuous Integration (CI) pipeline.
- **Custom Video Naming**: Investigate methods or workarounds for more descriptive video file names if required, potentially by moving/renaming files post-test execution via a script or Maven plugin.
- **Page Object Model (POM)**: For larger test suites, refactor to use the Page Object Model design pattern to improve maintainability and reduce code duplication.
- **Environment Configuration**: Externalize configurations like the application URL if it differs across environments. 