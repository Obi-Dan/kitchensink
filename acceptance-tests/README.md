# Kitchensink API Acceptance Tests

This module contains API acceptance tests for the Kitchensink Quarkus application. These tests verify the application's RESTful web services from an external client perspective.

## Overview

The primary goal of these tests is to ensure that the API endpoints behave as expected according to the defined business requirements and API specifications. They cover various scenarios, including successful operations, error handling, and validation.

## Technology Stack

*   **Java**: The programming language used for writing tests.
*   **REST Assured**: A Java DSL for simplifying testing of RESTful APIs.
*   **JUnit 5**: The testing framework used for structuring and running tests.
*   **Maven**: The build automation and dependency management tool.

## Test Structure

Tests are located in `src/test/java/org/jboss/as/quickstarts/kitchensink/acceptance/`. Key test classes typically focus on specific resources or functionalities, for example:

*   `MemberResourceAcceptanceTest.java`: Contains tests for the `/api/members` endpoints, covering creation, retrieval, and validation of member data.

## Running Tests

**Prerequisites:**

1.  The Kitchensink application (from the `app/` module) must be built and running. The main project `Makefile` handles this if using the `make acceptance-test` target.
2.  If running manually, ensure the application is accessible, typically at `http://localhost:8080`.

**Using the Main Project Makefile (Recommended):**

The easiest way to run these tests is via the main project's Makefile:

```bash
make acceptance-test
```
This target will typically:
1. Ensure the application is built and started (often using Docker).
2. Execute the acceptance tests against the running application.
3. Stop the application after tests complete.

**Running Manually with Maven:**

If the application is already running, you can execute the tests directly using Maven from within the `acceptance-tests/` directory:

```bash
mvn clean test
```

This will compile and run all tests within this module. Test reports will be generated in `target/surefire-reports/`.

## Key Areas Covered

*   **CRUD Operations**: Verifying Create, Read, Update, and Delete operations for members.
*   **Input Validation**: Ensuring API endpoints correctly validate input data and return appropriate error responses (e.g., 400 Bad Request).
*   **Business Rule Enforcement**: Testing for business-specific rules, such as preventing duplicate email registrations (e.g., 409 Conflict).
*   **Error Handling**: Checking for correct error codes and messages for various failure scenarios (e.g., 404 Not Found for non-existent resources).

## Future Considerations

*   Expand test coverage to include any new API endpoints or significant changes to existing ones.
*   Implement more sophisticated data setup and teardown strategies if tests require specific pre-existing data states.
*   Integrate these tests into a Continuous Integration (CI) pipeline. 