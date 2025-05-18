# Test Strategy and Coverage Analysis

## 1. Overview

This document outlines the testing strategy for the Kitchensink Quarkus application and provides an analysis of the current test coverage. The project employs a multi-layered testing approach:

*   **Unit Tests**: Focus on individual components (classes and methods) within the main application module (`app/`). Code coverage for these tests is measured using JaCoCo.
*   **API Acceptance Tests**: Verify the functionality of the application's REST APIs from an external perspective. These tests are located in the `acceptance-tests/` module.
*   **UI Acceptance Tests**: Validate end-to-end user scenarios through the web interface. These tests are located in the `ui-acceptance-tests/` module and utilize Playwright.

The primary goal for unit test coverage (JaCoCo) is to achieve at least 80% statement and branch coverage for the main application logic in `app/src/main/java`.

## 2. Running Tests

The `Makefile` provides convenient targets for executing the different types of tests:

*   **Unit Tests (with JaCoCo Coverage):**
    ```bash
    make test-coverage
    ```
    This command runs all unit tests in the `app/` module and generates a JaCoCo code coverage report.

*   **View Unit Test Coverage Report:**
    ```bash
    make test-report
    ```
    This opens the JaCoCo HTML report (from `app/target/jacoco-report/index.html`) in your browser.

*   **API Acceptance Tests:**
    ```bash
    make acceptance-test
    ```
    This starts the application via Docker and runs the API tests defined in `acceptance-tests/`.

*   **UI Acceptance Tests (Playwright):**
    ```bash
    make ui-test
    ```
    This starts the application and runs the Playwright UI tests defined in `ui-acceptance-tests/`. Videos of test executions are saved in `ui-acceptance-tests/target/videos/`.

*   **Run All Tests:**
    ```bash
    make test-all
    ```
    This executes `test-coverage`, `acceptance-test`, and `ui-test` sequentially.

## 3. Unit Test Coverage (JaCoCo)

This section details the code coverage achieved by the unit tests for the main application module (`app/src/main/java`), as measured by JaCoCo.

### 3.1. Current JaCoCo Coverage Summary

*(Note: This table reflects the state of unit test coverage. Run `make test-report` to get the latest figures after making code changes and running tests.)*

| Package                                         | Missed Instructions | Coverage | Missed Branches | Branches Coverage | Missed Complexity | Total Complexity | Missed Lines | Total Lines | Missed Methods | Total Methods | Missed Classes | Total Classes |
| ----------------------------------------------- | -------------------:|:--------:| ----------------:|:-----------------:| ------------------:|:----------------:| -------------:| ------------:| ---------------:|:-------------:| ---------------:|:-------------:|
| **Overall**                                     |    *348 of 385*     |  **10%** |   *14 of 14*     |       **0%**      |        *31*        |        41        |     *82*      |      96      |      *24*       |       34      |       *7*       |       9       |
| `org.jboss.as.quickstarts.kitchensink.model`    |      *3 of 34*      |   91%    |     *0 of 0*     |        n/a        |         *1*        |        10        |      *1*      |      14      |       *1*       |       10      |       *1*       |       2       |
| `org.jboss.as.quickstarts.kitchensink.rest`     |    *162 of 162*     |    0%    |   *10 of 10*     |        0%         |        *13*        |        13        |     *37*      |      37      |       *8*       |       8       |       *2*       |       2       |
| `org.jboss.as.quickstarts.kitchensink.service`  |     *23 of 23*      |    0%    |     *0 of 0*     |        n/a        |         *2*        |         2        |      *5*      |       5      |       *2*       |       2       |       *1*       |       1       |
| `org.jboss.as.quickstarts.kitchensink.util`    |      *6 of 9*      |   33%    |     *0 of 0*     |        n/a        |         *1*        |         2        |      *1*      |       2      |       *1*       |       2       |       *0*       |       1       |

*(Note: The `kitchensink.data` and `kitchensink.controller` packages from the original JaCoCo report were specific to the old JSF/EJB architecture and are not present in the Quarkus version. The coverage numbers above are illustrative and need to be updated by running `make test-report` after any significant test additions.)*

### 3.2. Analysis of Unit Test Coverage

*   **`kitchensink.model`**: Generally shows good coverage due to tests for entity validations and behavior.
*   **`kitchensink.rest`**: (TARGET FOR IMPROVEMENT) This package, containing `MemberResourceRESTService.java`, is a key area for increasing unit test coverage. Many of its methods and branches are currently not covered by unit tests.
*   **`kitchensink.service`**: (TARGET FOR IMPROVEMENT) The `MemberRegistration.java` service also requires more unit tests.
*   **`kitchensink.util`**: Has some coverage but can be improved.

The existing unit tests in `MemberRestResourceTest.java` primarily focus on testing the REST endpoints through HTTP calls (integration-style tests within a unit testing framework). While valuable, they don't always achieve fine-grained coverage of all conditional logic and branches within the service methods themselves without extensive and complex mocking of the entire request pipeline.

### 3.3. Recommendations for Improving Unit Test (JaCoCo) Coverage

To reach the 80%+ coverage goal for the `app` module:

1.  **`MemberResourceRESTService.java`**:
    *   Add focused unit tests for each method.
    *   Mock dependencies like `MemberRepository` and `MemberRegistration` (using `@InjectMock` or `@QuarkusMock` as appropriate).
    *   Test different paths through conditional logic (e.g., `if/else` blocks, exception handling `try/catch`).
    *   Specifically target uncovered lines and branches indicated by the JaCoCo report.
    *   Ensure validation logic (`validateMemberBean`, `createViolationResponse`) is thoroughly tested with various inputs.
    *   Cover different scenarios for UI methods like `getWebUi`, `getMemberByIdUi`, and `registerViaUi`, focusing on how they interact with services and prepare data for templates, rather than testing the template rendering itself (which is harder in pure unit tests).

2.  **`MemberRegistration.java`**:
    *   Unit test the `register()` method by mocking `MemberRepository` and any other dependencies.
    *   Verify correct interaction with the repository (e.g., `persistAndFlush` is called).
    *   Test behavior when `EmailAlreadyExistsException` is expected.

3.  **General Approach**:
    *   Regularly run `make test-report` to identify uncovered code sections.
    *   Write tests that specifically target these red and yellow lines in the JaCoCo report.
    *   Ensure tests cover not only successful paths ("happy path") but also error conditions, edge cases, and validation failures.

## 4. API Acceptance Tests (`acceptance-tests/`)

*   **Purpose**: These tests validate the application's REST API from an end-user/client perspective. They ensure that the API endpoints behave as expected according to the [BUSINESS_REQUIREMENTS.md](BUSINESS_REQUIREMENTS.md).
*   **Technology**: Java, JUnit, REST Assured.
*   **Scope**: Cover all public API endpoints in `MemberResourceRESTService.java` (`/api/members`, `/api/members/{id}`). Tests include:
    *   Successful data retrieval (GET).
    *   Successful data creation (POST).
    *   Validation error responses (e.g., 400 Bad Request for invalid input).
    *   Conflict error responses (e.g., 409 Conflict for duplicate email).
    *   Not Found responses (e.g., 404 for non-existent resources).
*   **Execution**: Run via `make acceptance-test`. This target manages starting the application in Docker and running the tests against it.

## 5. UI Acceptance Tests (`ui-acceptance-tests/`)

*   **Purpose**: These tests validate end-to-end user flows through the web interface (`/rest/app/ui`). They ensure the UI functions correctly and interacts properly with the backend services.
*   **Technology**: Java, Playwright.
*   **Scope**: Cover key user scenarios such as:
    *   Navigating to the registration page.
    *   Successfully registering a new member.
    *   Attempting to register a member with invalid data and verifying error messages.
    *   Attempting to register a member with an existing email and verifying error messages.
    *   Verifying that registered members appear in the member list.
    *   Checking for the "no members" message when the list is empty.
    *   Verifying links and basic page content.
*   **Execution**: Run via `make ui-test`. This target uses Docker Compose to build and orchestrate the application, database, and a dedicated test execution container. Test execution videos are captured.

## 6. Conclusion

The project has a foundational testing structure encompassing unit, API acceptance, and UI acceptance tests. The immediate focus for improving coverage lies in significantly increasing the unit test (JaCoCo) coverage for the `kitchensink.rest` and `kitchensink.service` packages within the `app` module. The detailed JaCoCo report (accessible via `make test-report`) should be the primary guide for identifying and addressing these coverage gaps. 