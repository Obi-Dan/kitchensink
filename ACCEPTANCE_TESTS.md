# Acceptance and UI Tests Overview

This document provides an overview of the acceptance and UI (User Interface) acceptance tests for the KitchenSink application. These tests are crucial for verifying that the application meets its specified business requirements from different perspectives.

## 1. Acceptance Tests (API/Service Level)

### Overview
-   **Location:** `acceptance-tests/`
-   **Purpose:** These tests focus on verifying the backend logic and REST API endpoints of the application without direct interaction with the UI. They ensure that the core services behave as expected, data is processed correctly, and API contracts are met.
-   **Technology:** Java, JUnit, REST Assured (or similar HTTP client for API testing).

### Functionality and Requirements Covered
The acceptance tests primarily validate the REST API functionalities, which correspond to the **Section 3 (REST API)** and parts of **Section 1 (Member Management)** and **Section 4 (Data Persistence)** of the `BUSINESS_REQUIREMENTS.md`. Key areas include:
-   **REQ-3.1.2 - REQ-3.1.4:** Creating, retrieving individual members, and retrieving all members via REST API.
-   **REQ-3.1.5:** Ensuring correct HTTP status codes are returned (e.g., 200 for success, 400 for bad requests, 404 for not found, 409 for conflicts).
-   **REQ-3.2.1 - REQ-3.2.4:** Validating API response formats (JSON), error message structures for validation failures, and conflict responses for duplicate emails.
-   **REQ-1.1.2:** Verification of unique email constraints at the API level.
-   **REQ-1.2.1 - REQ-1.2.3:** Server-side validation logic when data is submitted via the API.

### How to Run
1.  Ensure the application environment can be started (Docker is configured).
2.  From the project root directory, run:
    ```bash
    make acceptance-test
    ```
    This command will:
    -   Start the application using `docker-compose up -d app`.
    -   Wait for the application to be healthy.
    -   Execute the tests located in the `acceptance-tests/` directory using Maven (`cd acceptance-tests && mvn test`).
    -   Stop and clean up the application environment using `docker-compose down -v`.
-   Test reports are typically generated in `acceptance-tests/target/surefire-reports/`.

## 2. UI Acceptance Tests (End-to-End)

### Overview
-   **Location:** `ui-acceptance-tests/`
-   **Purpose:** These tests simulate real user interactions with the web interface of the application. They verify end-to-end user flows, ensuring that the UI components function correctly and that the integration between the UI, backend services, and database is working as expected.
-   **Technology:** Java, JUnit, Playwright.

### Functionality and Requirements Covered
The UI tests primarily validate the web interface functionalities, which correspond to **Section 2 (User Interface)** and also touch upon successful execution of requirements from **Section 1 (Member Management)** through the UI. Key areas include:
-   **REQ-2.1.1 - REQ-2.1.11:** All aspects of the web interface, including:
    -   Member registration form (REQ-2.1.5).
    -   Display and correctness of validation messages next to fields and globally (REQ-2.1.2, REQ-2.1.4, REQ-2.1.6, REQ-2.1.7).
    -   Display of members in a table, including content, sorting, and "No members" message (REQ-2.1.3, REQ-2.1.8, REQ-2.1.9, REQ-1.3.1).
    -   Functionality of REST URL links presented in the UI (REQ-2.1.10, REQ-2.1.11).
-   **REQ-1.1.1 - REQ-1.1.3:** Member registration process via the UI, including successful registration and data validation.
-   **REQ-1.2.1 - REQ-1.2.4:** User experience of data validation rules for name, email, and phone number, including error message display.
-   Verification of page structure, navigation elements, and external links.

### How to Run
1.  Ensure Docker and Docker Compose are installed and running.
2.  From the project root directory, run:
    ```bash
    make ui-test
    ```
    This command will:
    -   Clean up old UI test video recordings.
    -   Build the Docker images for the application (`app`) and the UI tests (`ui-tests`) if they are not already built or are outdated.
    -   Start the application service (`app`) in detached mode.
    -   Run the UI acceptance tests within a dedicated Docker container (service `ui-tests`). The tests connect to the `app` service over the Docker network.
    -   Stop and remove all services and related volumes after tests complete.
-   **Outputs:**
    -   Test reports are generated in `ui-acceptance-tests/target/surefire-reports/`.
    -   Video recordings of each test execution are saved in `ui-acceptance-tests/target/videos/`.
    -   HTML snapshots of the page at various test steps are saved in `ui-acceptance-tests/target/html-snapshots/`.
    -   You can use `make open-video-dir` to quickly open the video directory.

## Purpose and Function Summary

-   **Acceptance Tests** ensure the backend (API, services) is robust, correct, and adheres to its contracts, forming a critical layer of defense against regressions in business logic and data handling.
-   **UI Acceptance Tests** validate the complete user experience from the browser's perspective, ensuring that all parts of the application (frontend, backend, database) work together harmoniously to deliver the required functionalities to the end-user.

Both sets of tests are vital for maintaining application quality, enabling confident refactoring, and ensuring that business requirements are consistently met through automated verification. 