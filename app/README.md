# Kitchensink Quarkus Application (app/)

This directory contains the main Kitchensink application, migrated to and built with Quarkus.

## Overview

This is a Quarkus-based web application that provides a simple member registration and management system. It demonstrates how to build modern, cloud-native Java applications using the Quarkus framework. The application exposes RESTful APIs for member management and a simple web UI for interaction.

It serves as the core backend for the Kitchensink project, providing the business logic and data persistence layer.

## Technology Stack

*   **Quarkus**: The core framework for building the application. ([https://quarkus.io/](https://quarkus.io/))
*   **Java**: The primary programming language.
*   **Maven**: For build automation and dependency management.
*   **RESTEasy Reactive (JAX-RS)**: For creating RESTful web services.
*   **Quarkus Panache**: For simplified data access with MongoDB.
*   **MongoDB**: The NoSQL database used for persistence (via the `quarkus-mongodb-panache` extension).
*   **ArC (CDI)**: For dependency injection.
*   **Qute**: The templating engine used for rendering the web UI.
*   **Hibernate Validator**: For data validation using bean validation annotations.
*   **JaCoCo**: For code coverage reporting, integrated via the `quarkus-jacoco` extension.
*   **Spotless**: For code formatting, integrated via the Spotless Maven Plugin.
*   **Docker**: The application is designed to be easily containerized, with a `Dockerfile` provided in this directory.

## Application Structure

Key packages and their roles:

*   `org.jboss.as.quickstarts.kitchensink.model`: Contains the data model (e.g., `Member.java` Panache entity).
*   `org.jboss.as.quickstarts.kitchensink.rest`: Houses the JAX-RS resource classes (e.g., `MemberResourceRESTService.java`) that define the API and UI endpoints.
*   `org.jboss.as.quickstarts.kitchensink.service`: Includes service beans that encapsulate business logic (e.g., `MemberRegistration.java`).
*   `org.jboss.as.quickstarts.kitchensink.util`: Utility classes.
*   `src/main/resources/application.properties`: Main configuration file for the Quarkus application, including database connections, logging levels, and application-specific settings.
*   `src/main/resources/templates/`: Contains Qute template files for the web UI.
*   `src/test/java/`: Contains unit and integration tests for the application code.

## Building and Running the Application

While this module can be built and run independently, it's **highly recommended** to use the main project `Makefile` located in the root directory for a streamlined experience, as it handles Docker Compose setup for the application and MongoDB.

**Using the Main Project Makefile (Recommended):**

Refer to the main `README.md` in the project root for commands like:
*   `make launch`: Builds, runs the application and MongoDB in Docker, and opens the UI.
*   `make start-clean`: Cleans data, builds, and runs the application and MongoDB in Docker.
*   `make start`: Builds and runs if not already done.
*   `make stop`: Stops the Docker containers.

**Running in Development Mode (Live Coding):**

From the `app/` directory, you can run the Quarkus application in development mode, which enables live reloading:

```bash
mvn quarkus:dev
```

This typically requires a MongoDB instance to be running and accessible as configured in `application.properties` (e.g., `quarkus.mongodb.connection-string`). The top-level `docker-compose.yml` can be used to start a MongoDB instance separately if needed.

**Building a JAR/Native Executable:**

From the `app/` directory:

*   To build a standard JAR:
    ```bash
    mvn clean package
    ```
    The output will be in `target/quarkus-app/`.

*   To build a native executable (requires GraalVM setup):
    ```bash
    mvn clean package -Pnative
    ```
    The output will be in `target/`.

## Configuration

Primary configuration is done through `src/main/resources/application.properties`. This file includes settings for:
*   Database connection (`quarkus.mongodb.*`)
*   Web server (e.g., `quarkus.http.port`)
*   Application path (`quarkus.resteasy-reactive.path`)
*   Logging levels
*   JaCoCo settings (`quarkus.jacoco.enabled`)

Profiles (e.g., for `dev`, `test`, `prod`) can be used to customize configurations for different environments (e.g., `%dev.quarkus.mongodb.connection-string`).

## Testing

Unit and integration tests for this module are located in `src/test/java/`.

*   **Run tests (including style checks via Spotless):**
    From the `app/` directory:
    ```bash
    mvn clean test
    ```
    Or use the top-level Makefile:
    ```bash
    make test
    ```

*   **Run tests with code coverage (JaCoCo):**
    From the `app/` directory:
    ```bash
    mvn clean verify
    ```
    Or use the top-level Makefile:
    ```bash
    make test-coverage
    make test-report
    ``` 