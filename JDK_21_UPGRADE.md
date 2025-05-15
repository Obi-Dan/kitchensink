# JDK 21 Upgrade and Refactoring Log

This document tracks the steps taken to upgrade the Kitchensink application to JDK 21 and refactor the codebase according to JDK 21 best practices, SOLID architecture principles, and Lean architecture principles.

## Phase 1: JDK 21 Update & Basic Compatibility

### 1.1 Create `JDK_21_UPGRADE.md`
- This document was created to log the upgrade and refactoring process.

### 1.2 Update `app/pom.xml` for JDK 21
- Added the following properties to `app/pom.xml` to set the Java version to 21:
  ```xml
  <maven.compiler.source>21</maven.compiler.source>
  <maven.compiler.target>21</maven.compiler.target>
  <maven.compiler.release>21</maven.compiler.release>
  ``` 

### 1.3 Investigate WildFly/EAP Compatibility for JDK 21 Runtime
- The project uses `jboss-eap-ee-with-tools` BOM version `8.0.0.GA-redhat-00009`, corresponding to JBoss EAP 8.0.0.GA.
- JBoss EAP 8.x is primarily supported and tested with Java SE 11 and SE 17.
- Running JBoss EAP 8.0.0.GA on JDK 21 is not officially supported by Red Hat and may lead to runtime issues, especially for integration tests using a managed Arquillian container or for deployment.
- The `eap-maven-plugin` (`1.0.0.Final-redhat-00014`) provisions this server based on EAP 8 Galleon layers (`org.jboss.eap:wildfly-ee-galleon-pack`).

**Considerations for Runtime/Integration Tests:**
- **Option 1 (Proceed with EAP 8 on JDK 21):** High risk of runtime instability due to lack of official support.
- **Option 2 (Switch to WildFly Community):** Change Galleon feature packs to a WildFly community version (e.g., WildFly 30+) that officially supports JDK 21. This moves away from JBoss EAP bits.
- **Option 3 (Alternative Integration Test Strategy):** Use remote Arquillian tests against an EAP/WildFly instance running on a compatible JDK (e.g., 17) in a separate environment (like Docker).

**Decision for now:** Proceed with JDK 21 for compilation and unit tests. Defer final decision on runtime environment and integration test strategy until those phases are reached. The focus is first on code compilation and local unit test execution with JDK 21.

### 1.4 Attempt Initial Build (Compilation)
- Ran `mvn clean compile` in the `app` directory.
- Compilation was successful with JDK 21 (`<maven.compiler.release>21</maven.compiler.release>`). The `maven-compiler-plugin` version used was 3.8.1.

### 1.5 Attempt to Run Unit Tests
- Ran `mvn test` in the `app` directory.
- All 12 unit tests passed successfully.
- The `jacoco-maven-plugin` (version `0.8.10`) also executed successfully, generating a coverage report.
- No "Unsupported class file major version" errors were observed during this phase, suggesting JaCoCo 0.8.10 is sufficient for unit test coverage instrumentation with JDK 21 compiled classes in this project.

## Phase 2: JDK 21 Language Features Refactoring

### 2.1 Refactor `rest/MemberResourceRESTService.java`
- Applied `var` for local variable declarations in `lookupMemberById`, `createMember`, `validateMember`, and `createViolationResponse` methods.
- Refactored the `createMember` method to return `Response` objects directly from try/catch blocks, eliminating the intermediate `Response.ResponseBuilder` variable.
- Removed unused import `java.util.Map`.
- Verified changes by running `mvn test`, which passed successfully (including Checkstyle).

### 2.2 Refactor `controller/MemberController.java`
- Applied `var` for local variable declarations in `register` and `getRootErrorMessage` methods.
- Corrected a type inference issue where `var t = e;` (e being `Exception`) was incompatible with `t = t.getCause();` (which returns `Throwable`). Reverted to `Throwable t = e;` for this variable.
- Verified changes by running `mvn test`, which passed successfully. 

### 2.3 Data Access Refactoring (`data/MemberRepository.java`)

*   Applied `var` for local variable declarations (`CriteriaBuilder`, `CriteriaQuery`, `Root`).
*   Switched from string-based property names in Criteria API to type-safe JPA metamodel (`Member_.email`, `Member_.name`).
*   Added `hibernate-jpamodelgen` as an annotation processor in `app/pom.xml` by:
    *   Updating the `maven-compiler-plugin` version to `3.13.0` by setting the `<version.compiler.plugin>` property.
    *   Ensuring `hibernate-jpamodelgen` and `hibernate-validator-annotation-processor` were listed in `<annotationProcessorPaths>` within the `maven-compiler-plugin` configuration.
    *   Ensuring `hibernate-jpamodelgen` dependency had `<scope>provided</scope>`.
*   Resolved compilation error `cannot find symbol class Member_`.
*   Removed unused imports for `CriteriaBuilder`, `CriteriaQuery`, `Root`, and `java.util.logging.Logger` to satisfy Checkstyle.
*   `mvn clean test` confirmed changes were successful, Checkstyle passed, and all unit tests passed.
*   ~~**Note**: A non-fatal JaCoCo error (`Unsupported class file major version 67`) was observed during test execution when instrumenting JDK internal classes. This did not cause test failures.~~ This issue was resolved by upgrading JaCoCo version to `0.8.12` in `app/pom.xml`.

### 2.4 JAX-RS Activator Analysis (`rest/JaxRsActivator.java`)

*   Analyzed `JaxRsActivator.java`.
*   No applicable JDK 21 refactorings were identified. The class is a simple JAX-RS activator and already concise.
*   Considered to align well with SOLID and Lean principles.

### 2.5 Utilities Refactoring (`util/Resources.java`)

*   Analyzed `Resources.java`.
*   No applicable JDK 21 refactorings were identified. The class produces an `EntityManager` and `Logger` and is already concise.
*   Considered to align well with SOLID and Lean principles.

## Phase 3: SOLID and Lean Principles Review (Ongoing)

This phase involves reviewing components for adherence to SOLID and Lean architectural principles.

### 3.1 Model Review (`model/Member.java`)

*   **SRP**: Adheres well within idiomatic Jakarta EE usage. Defines the "Member" concept for persistence, REST, and validation.
*   **OCP/LSP/ISP/DIP**: Largely not applicable or met by its nature as a data entity.
*   **Lean Principles**: Generally lean and focused. Bean Validation and JPA annotations build integrity in.
*   **Considerations**: Standard JPA entity considerations like immutability (hard with JPA), `serialVersionUID` (if heavy serialization is used), and `equals()`/`hashCode()` implementation (if used in sets or detached scenarios) apply but are not critical issues for the current scope. No changes made.

### 3.2 Service Review (`service/MemberRegistration.java`)

*   **SRP**: Adheres well. Single responsibility of registering a member (logging, persisting, firing event).
*   **OCP**: Good. Uses events for extension, aligning with OCP.
*   **DIP**: Good. Depends on abstractions (`Logger`, `EntityManager`, `Event`).
*   **Lean Principles**: Concise, uses EJB for transaction management, event-driven.
*   **Considerations**:
    *   The `register` method `throws Exception`, which is too generic. More specific exception handling (e.g., custom application exceptions or specific `PersistenceException`s) would be better in a production application.
    *   Logging uses string concatenation; parameterized logging is generally preferred.
*   No changes made at this time, but points noted for potential future refinement.

### 3.3 REST API Review (`rest/MemberResourceRESTService.java`)

*   **SRP**: Adheres well. Manages Member REST endpoints, HTTP concerns, validation, service delegation, and response formatting.
*   **OCP**: Standard for REST controllers; new operations would be new methods.
*   **DIP**: Good. Depends on injected abstractions/services.
*   **Lean Principles**: Focused methods. Proactive email check is a trade-off for better error messaging over relying solely on DB constraint exceptions.
*   **Changes Made**:
    *   Changed visibility of `emailAlreadyExists` method from `public` to `private` as it's only used internally.
    *   Updated logging in `createViolationResponse` to use parameterized logging.
*   **Considerations**:
    *   Generic `catch (Exception e)` in `createMember` could be refined to differentiate client errors (4xx) from server errors (5xx) more accurately and log server errors more thoroughly.
    *   The public visibility of `emailAlreadyExists` was changed to private. If external use or direct testing of this method was intended, this change would need reconsideration.
*   `mvn clean test` confirmed changes were successful.

### 3.4 JSF Controller Review (`controller/MemberController.java`)

*   **SRP**: Adheres well for a JSF backing bean. Manages UI interaction for new member registration, delegates to a service, and handles FacesMessages.
*   **DIP**: Good. Depends on injected `FacesContext` and `MemberRegistration` service.
*   **Lean Principles**: Standard JSF controller structure. `getRootErrorMessage` attempts to provide detailed error feedback.
*   **Considerations**:
    *   Tightly coupled to JSF. Reusability outside a JSF context is limited.
    *   `getRootErrorMessage` might expose overly technical error details to the UI. Mapping specific exceptions to user-friendly messages would be more robust.
    *   Relies on JSF lifecycle for bean validation of `newMember` before the `register()` action.
*   No changes made. Its current structure is typical for a JSF backing bean in a quickstart.

### 3.5 Data Producer Review (`data/MemberListProducer.java`)

*   **SRP**: Adheres well. Produces a list of members for UI consumption and manages refreshing this list based on events.
*   **OCP**: Good use of event observation for refreshing data.
*   **DIP**: Good. Depends on injected `MemberRepository`.
*   **Lean Principles**: Focused, uses CDI features effectively (`@PostConstruct`, `@Observes` with `Reception.IF_EXISTS`). Data is fetched per request, ensuring reasonable freshness for a request-scoped bean.
*   No changes made. The class is well-structured for its role.

### 3.6 Data Repository Review (`data/MemberRepository.java`)

*   **SRP**: Adheres very well. Encapsulates data access logic for `Member` entities with focused query methods.
*   **OCP**: Good. New queries would be new methods.
*   **DIP**: Good. Depends on `EntityManager` abstraction. Acts as an abstraction for its clients.
*   **Lean Principles**: Clean, direct use of JPA Criteria API with type-safe queries. No unnecessary complexity.
*   **Considerations**:
    *   Could implement an interface for better testability of its clients in larger systems, but not critical here.
    *   Callers of `findByEmail` must handle `NoResultException` if the email is not found.
*   No changes made. The class is robust and clean.

### 3.7 JAX-RS Activator Review (`rest/JaxRsActivator.java`)

*   Re-confirmed from Phase 2.4 analysis: This class is extremely simple and focused.
*   **SRP & Lean**: Adheres perfectly. Its sole purpose is JAX-RS activation via annotation.
*   No changes needed.

### 3.8 Utilities/Resource Producer Review (`util/Resources.java`)

*   Re-confirmed from Phase 2.5 analysis: This class is simple and provides CDI producers for `EntityManager` and `Logger`.
*   **SRP & Lean**: Adheres well. Focused on producing shared resources.
*   No changes needed.

## Phase 4: Acceptance Testing with `make acceptance-test`

This phase focuses on getting the `make acceptance-test` command to pass with the application compiled for JDK 21.

### 4.1 Investigation of `make acceptance-test`

*   The `Makefile` in the project root defines the `acceptance-test` target.
*   It uses `docker-compose up -d` to start the application.
*   The `docker-compose.yml` file defines a `wildfly` service that builds an image using `app/Dockerfile`.
*   The `app/Dockerfile` (original state) used `quay.io/wildfly/wildfly:28.0.1.Final-jdk11` as its base image.
*   The acceptance tests themselves are run from the `acceptance-tests/` directory using `mvn test`.

**Core Issue Identified:** WildFly 28.0.1 running on JDK 11 (from the base image) will not support an application compiled for JDK 21 (`UnsupportedClassVersionError` expected).

### 4.2 Plan to Update Docker Environment

1.  **Modify `app/Dockerfile`**: Change the base image to a WildFly version that runs on and supports JDK 21. Target: `quay.io/wildfly/wildfly:32.0.1.Final-jdk21`.
2.  **Build and Test**: 
    *   Run `make clean` to ensure old Docker images are removed.
    *   Run `make acceptance-test` to build the new image, start the container, and execute the tests.
    *   Address any issues that arise during deployment or test execution.

## Phase 5: Final Review and Documentation

*   Perform a final review of all changes.
*   Ensure all documentation is up-to-date.
*   Merge changes. 