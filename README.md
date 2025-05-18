# Kitchensink Application (Quarkus Edition)

## Project Overview

The "kitchensink" project is a demonstration application originally designed as a Java Enterprise Application (Jakarta EE) quickstart for JBoss Enterprise Application Platform (EAP). This version has been migrated to **Quarkus**, a Supersonic Subatomic Java Framework, to showcase modern cloud-native Java development. It utilizes **MongoDB** as its persistent data store.

The application implements a simple member registration and management system, allowing users to add, view, and validate member information. It serves as a learning tool and a starting point for developers building enterprise applications with Quarkus.

Most of the migration and modernization effort for this project was performed with the assistance of **Generative AI**, highlighting its capabilities in accelerating software development and technology transitions.

## Developer Quick Start: Building and Running the Application

This application is containerized using Docker and managed with `docker-compose`. The `Makefile` provides convenient targets for common development tasks.

**Prerequisites:**
*   Docker and Docker Compose installed.
*   Java Development Kit (JDK) 21.
*   Apache Maven.

**Recommended Way to Start (Build, Run, and Open UI):**

1.  **Clone the repository.**
2.  **Navigate to the project root directory.**
3.  **Launch the application:**
    ```bash
    make launch
    ```
    This command will:
    *   Ensure any existing MongoDB data volume is purged (for a clean start).
    *   Build the application Docker image (which includes compiling the Quarkus app via Maven).
    *   Start the application and its MongoDB database using `docker-compose`.
    *   Wait a few seconds for the application to initialize.
    *   Automatically open the application's web UI (`http://localhost:8080/rest/app/ui`) in your default browser.

**Alternative Start Commands (if you don't need to open the UI automatically or want more control):**

*   **Start with a clean database (without auto-opening UI):**
    ```bash
    make start-clean
    ```
*   **Start without cleaning the database (if already built and run before, without auto-opening UI):**
    ```bash
    make start
    ```

**Accessing the application (if not using `make launch`):**
*   The web UI will be available at: `http://localhost:8080/rest/app/ui`
*   The REST API base will be: `http://localhost:8080/rest/app/api`

## Migration from JBoss EAP (WildFly) / Jakarta EE

This project was migrated from a traditional Jakarta EE architecture (utilizing technologies like JSF, EJBs, and deploying as a WAR to WildFly) to a streamlined Quarkus application. The migration focused on:

*   Replacing Jakarta EE components with their Quarkus equivalents or idiomatic Quarkus approaches (e.g., using Panache for persistence, RESTEasy Reactive for JAX-RS, Qute for templating).
*   Leveraging Quarkus's build-time optimizations and live coding features.
*   Containerizing the application using Docker with a Quarkus-optimized image.
*   Simplifying the deployment and configuration.

For a detailed log of the migration process, decisions made, and challenges encountered, please refer to the [MIGRATION_DIARY.md](docs/MIGRATION_DIARY.md).

To understand the original architecture before migration, see:
*   [ORIGINAL_ARCHITECTURE.md](docs/ORIGINAL_ARCHITECTURE.md)
*   [ORIGINAL_UI_SEQUENCE.md](docs/ORIGINAL_UI_SEQUENCE.md) (for the UI interaction flow)

## Additional Documentation

The `docs/` folder contains further information about the project, its history, and development practices:

*   [**ACCEPTANCE_TESTS.md**](docs/ACCEPTANCE_TESTS.md): Details on the strategy and execution of API and UI acceptance tests.
*   [**BUSINESS_REQUIREMENTS.md**](docs/BUSINESS_REQUIREMENTS.md): Outlines the business goals and functional requirements for the application.
*   [**CODING_STANDARDS.md**](docs/CODING_STANDARDS.md): Specifies coding conventions and style guidelines adopted for this project.
*   [**MIGRATION_DIARY.md**](docs/MIGRATION_DIARY.md): A chronological log of the migration from JBoss EAP/Jakarta EE to Quarkus, including challenges and solutions.
*   [**MIGRATION_PLAN.md**](docs/MIGRATION_PLAN.md): Describes the overall plan and approach taken for the application migration.
*   [**ORIGINAL_ARCHITECTURE.md**](docs/ORIGINAL_ARCHITECTURE.md): An overview of the system architecture of the original JBoss EAP/Jakarta EE application.
*   [**ORIGINAL_PROJECT_EVALUATION.md**](docs/ORIGINAL_PROJECT_EVALUATION.md): An evaluation of the original project before migration, covering its state, and technologies.
*   [**ORIGINAL_TESTS.md**](docs/ORIGINAL_TESTS.md): Information about the testing strategies and specific tests present in the original version of the application.
*   [**ORIGINAL_UI_SEQUENCE.md**](docs/ORIGINAL_UI_SEQUENCE.md): Explanations and diagrams of user interface flows in the original JBoss EAP/Jakarta EE application.
*   [**TEST_COVERAGE.md**](docs/TEST_COVERAGE.md): Details the overall test strategy (unit, API, UI), instructions for running tests, and analysis of unit test coverage with JaCoCo.

## Testing the Application

The project includes unit tests for the application logic and REST endpoints.

**Running Unit Tests:**

*   **Run all unit tests and style checks:**
    ```bash
    make test
    ```
    This command executes Maven to clean the project, run tests (which includes Spotless style checking), and compile the application. The `format` target is automatically run first to ensure code is formatted.

*   **Run unit tests with code coverage:**
    ```bash
    make test-coverage
    ```
    This command runs `mvn clean verify`, which executes tests and generates a JaCoCo code coverage report. Quarkus's `quarkus-jacoco` extension is used for this.

*   **View the coverage report:**
    ```bash
    make test-report
    ```
    This command first ensures `test-coverage` has been run, then attempts to open the HTML coverage report (`app/target/jacoco-report/index.html`) in your default web browser.

**Acceptance Tests:**

*   **Run API acceptance tests:**
    ```bash
    make acceptance-test
    ```
    This target starts the application via Docker, runs a suite of acceptance tests (located in the `acceptance-tests/` directory) against the running application's API, and then stops the application.

*   **Run UI acceptance tests (Playwright):**
    ```bash
    make ui-test
    ```
    This target builds and starts the necessary services (app, mongo, ui-tests container) via Docker Compose, runs UI tests using Playwright (defined in `ui-acceptance-tests/`), and then stops the services. Videos of the test runs are saved in `ui-acceptance-tests/target/videos/`.

*   **Open UI test video directory:**
    ```bash
    make open-video-dir
    ```
    This opens the directory containing videos recorded during UI acceptance tests.

*   **Run all tests (unit, coverage, acceptance, UI):**
    ```bash
    make test-all
    ```

## Makefile Targets Overview

The `Makefile` provides several targets to simplify common development tasks:

*   `build`: Builds the Docker image for the application.
*   `run`: Runs the application using `docker-compose` (assumes image is built).
*   `start`: A convenience target that runs `build` then `run`.
*   `stop`: Stops the running application (via `docker-compose down`).
*   `logs`: Shows the logs of the running application container.
*   `clean`: Stops the application and removes all related Docker containers, volumes, and the locally built application image.
*   `purge-mongo-data`: Stops and removes the MongoDB container and its data volume.
*   `start-clean`: Runs `purge-mongo-data` then `start` for a fresh application start.
*   `format`: Runs the Spotless code auto-formatter on the application code (`app/` directory). This is automatically run before `build`, `run`, `test`, `test-coverage`, and `acceptance-test`.
*   `launch`: Runs `start-clean` to ensure a fresh environment, then starts the application and attempts to open the UI (`http://localhost:8080/rest/app/ui`) in your default browser. **This is the recommended target for quickly getting the application running for development or demonstration.**
*   `test`: Runs unit tests and style checks.
*   `test-coverage`: Runs unit tests and generates a code coverage report.
*   `test-report`: Opens the code coverage report.
*   `acceptance-test`: Runs API acceptance tests.
*   `ui-test`: Runs UI acceptance tests.
*   `open-video-dir`: Opens the UI test video directory.
*   `test-all`: Runs all defined tests.
*   `help`: Displays a summary of available make targets.

## Project Requirements

The business and functional requirements for this application are detailed in [BUSINESS_REQUIREMENTS.md](docs/BUSINESS_REQUIREMENTS.md). 