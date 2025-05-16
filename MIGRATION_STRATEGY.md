# Kitchensink Application Migration Strategy

## 1. Introduction and Goals

This document outlines the strategy for migrating the Kitchensink Jakarta EE application to a modern, cloud-native stack.

**Primary Goals:**

1.  **Modernize the Stack:** Upgrade to JDK 21, replace JBoss EAP with a modern framework (Quarkus), and switch the database from H2 (via JPA/Hibernate) to MongoDB.
2.  **Like-for-Like Functionality:** The migrated application's UI and API behavior must be indistinguishable from the original for end-users. All existing features and endpoints must be preserved. Additionally, the UI must be exactly the same as the previous UI - styling, images, links, and behavior with no changes, additions, or modifications including all routes.
3.  **Maintainability and Quality:** Employ robust software engineering principles (SOLID, DRY, Clean Architecture, LEAN) to ensure a high-quality, maintainable codebase.
4.  **Containerization for Testing:** Utilize Docker for running MongoDB during acceptance tests.
5.  **Risk Mitigation:** Proactively identify and mitigate risks throughout the migration process.
6.  **Developer Experience:** Improve developer experience with auto-formatting and modern tooling.
7.  **Comprehensive Decision Logging:** Document every significant decision made during the migration process, including the options considered, the final resolution, and the rationale, ensuring alignment with all other primary goals in a MIGRATION_DIARY readme.

## 2. Technology Choices

### 2.1. Java Development Kit (JDK)
*   **Target:** JDK 21 (LTS).
*   **Rationale:** Leverage the latest Java features, performance improvements, and long-term support.

### 2.2. Application Framework: Quarkus
*   **Recommendation:** Quarkus.
*   **Rationale:**
    *   **Supersonic Subatomic Java:** Quarkus is designed for fast boot times and low memory usage, making it ideal for containerized environments and serverless deployments.
    *   **Developer Joy:** Offers live coding, unified configuration, and a rich extension ecosystem.
    *   **Reactive and Imperative:** Supports both programming models.
    *   **Standards-Based:** Leverages Jakarta EE standards (like CDI, JAX-RS) where applicable, which can ease the conceptual transition for some parts of the existing application, even if underlying implementations change.
    *   **Native Compilation:** Potential for GraalVM native image compilation for further performance gains (though not an initial primary goal, it offers future benefits).
    *   **Excellent MongoDB Support:** Panache for MongoDB provides an active record pattern and repository support, simplifying data access.

### 2.3. Database: MongoDB
*   **Target:** MongoDB.
*   **Rationale:**
    *   **Scalability and Flexibility:** Document-based model offers flexibility for evolving data structures.
    *   **Performance:** Optimized for high-volume read/write operations.
    *   **Developer-Friendly:** APIs and tools are generally considered intuitive.
    *   **Quarkus Integration:** Well-supported in the Quarkus ecosystem.

## 3. JDK 21 Upgrade Strategy

1.  **Update `pom.xml`:**
    *   Set `maven.compiler.source` and `maven.compiler.target` (or `maven.compiler.release`) to `21`.
    *   Ensure all dependencies, especially the chosen framework (Quarkus) and build plugins, are compatible with JDK 21.
2.  **Code Review and Adaptation:**
    *   Address any deprecated APIs or language features removed/changed between the current Java version and JDK 21.
    *   Leverage new JDK 21 features where appropriate (e.g., records, pattern matching for switch, virtual threads if applicable for specific I/O bound tasks, though Quarkus manages its own concurrency models).
3.  **Tooling:**
    *   Ensure IDEs, build tools, and CI/CD pipelines are configured for JDK 21.
*   **Risk:** Library incompatibilities.
*   **Mitigation:** Research Quarkus and other key dependency compatibility with JDK 21 beforehand. Test compilation and basic functionality early.

## 4. Application Layer Migration Strategy (to Quarkus & MongoDB)

This migration will be performed layer by layer, referencing `ORIGINAL_ARCHITECTURE.md`.

### 4.1. Domain Model (`model/Member.java`)

*   **Current:** JPA Entity.
*   **Target:** Quarkus Panache MongoDB Entity.
    *   Replace JPA annotations (`@Entity`, `@Id`, `@GeneratedValue`, `@Table`, `@Column`) with Panache MongoDB annotations (e.g., `@MongoEntity`, potentially `BsonId` for the ID).
    *   The `id` field can be a `String` (ObjectId hex string) or `org.bson.types.ObjectId`.
    *   Bean Validation annotations (`jakarta.validation.constraints.*`) remain compatible and should be preserved.
    *   `@XmlRootElement` is irrelevant for MongoDB persistence but might be kept if the class is directly used in JAX-RS responses (though DTOs are preferred). For direct use, ensure JAX-B or JSON-B (Quarkus uses JSON-B by default via Jackson) can serialize it.
*   **Data Modeling for MongoDB:**
    *   The `Member` entity is simple and translates well to a single MongoDB document. No complex embedding or referencing strategies are immediately needed.
    *   The unique constraint on `email` needs to be enforced at the MongoDB level (create a unique index on the `email` field in the `members` collection).
*   **Immutability:** Consider making fields `final` and using constructor initialization if the entity is primarily read after creation or if updates are handled by creating new instances (less common for entities but good for DTOs). For Panache entities, setters are often used by the framework.

### 4.2. Data Access Layer (DAL)

*   **Current:** `data/MemberRepository.java` (JPA Criteria API), `data/MemberListProducer.java` (CDI Producer).
*   **Target:** Quarkus Panache MongoDB Repository/Active Record pattern.
    *   **`MemberRepository`:**
        *   Replace with a PanacheMongoRepository or make `Member` a PanacheMongoEntity (Active Record).
        *   `findById(Long id)` becomes `findById(ObjectId id)` or `findById(String id)`.
        *   `findByEmail(String email)` can be implemented as a custom method in the repository or using Panache's query capabilities: `Member.find("email", email).firstResult()`.
        *   `findAllOrderedByName()`: `Member.list("ORDER BY name")` or a more MongoDB-idiomatic sort if using native queries.
        *   `EntityManager` injection is removed.
    *   **`MemberListProducer`:**
        *   The concept of producing a `@Named("members")` list for JSF EL is JSF-specific.
        *   For a new UI (see Presentation Layer), data will be fetched and provided to the template engine by the new controller/resource.
        *   The event-driven refresh (`onMemberListChanged`) needs to be re-implemented. If the UI is reactive (e.g., using WebSockets or SSE with Quarkus), this event can trigger a push to clients. Otherwise, client-side polling or a refresh action might be needed.
*   **Database Interaction:** Switch from JPA to MongoDB Java Driver via Panache.

### 4.3. Service Layer (`service/MemberRegistration.java`)

*   **Current:** Stateless EJB.
*   **Target:** Quarkus CDI Bean (`@ApplicationScoped` or `@Singleton`).
    *   Replace `@Stateless` with `@ApplicationScoped`.
    *   The core logic `em.persist(member)` becomes `member.persist()` (if Active Record) or `memberRepository.persist(member)`.
    *   **Transaction Management:** If operations need to be atomic (though a single persist is usually atomic in MongoDB at the document level), Quarkus `@Transactional` can be used if a JTA transaction manager is configured, but it's less common for typical MongoDB operations unless spanning multiple services or requiring rollback for other resources. For simple persists, MongoDB's atomicity is usually sufficient.
    *   **CDI Event Firing:** `jakarta.enterprise.event.Event` is still available in Quarkus. The event `memberEventSrc.fire(member)` can remain largely the same. The observer in the new `MemberListProducer` equivalent will need to be adapted.
*   **Logging:** `jakarta.inject.Inject private Logger log;` can still be used with Quarkus, which integrates JBoss Logging.

### 4.4. REST API Layer (`rest/`)

*   **Current:** JAX-RS (using RESTEasy in WildFly).
*   **Target:** Quarkus JAX-RS (using RESTEasy Reactive).
    *   `JaxRsActivator.java` (`@ApplicationPath("/rest")`): Can remain the same or be configured via `application.properties` in Quarkus (`quarkus.resteasy-reactive.path=/rest`).
    *   `MemberResourceRESTService.java`:
        *   `@Path("/members")`, `@RequestScoped` can remain.
        *   Methods (`listAllMembers`, `lookupMemberById`, `createMember`) and their JAX-RS annotations (`@GET`, `@POST`, `@Path`, `@PathParam`, `@Produces`, `@Consumes`) are directly compatible.
        *   Dependency injections (`Validator`, `MemberRepository` (adapted), `MemberRegistration` (adapted)) will work with Quarkus CDI.
        *   **Validation:** `Validator.validate(member)` remains the same. The `ConstraintViolationException` handling is standard.
        *   **Error Handling:** The `createViolationResponse` and email uniqueness check logic (`emailAlreadyExists`) will largely remain, but the repository call for `findByEmail` will change to the MongoDB version.
        *   `WebApplicationException` for 404 is standard.
*   **API Contract:** Crucially, ensure all request/response payloads, status codes, and paths remain *identical* to achieve the like-for-like goal.

### 4.5. Presentation Layer (`index.xhtml`, `MemberController.java`)

*   **Current:** JSF with Facelets, CDI `@Model` for `MemberController`.
*   **Target:** This is the most significant change. JSF is not a primary choice for Quarkus.
    *   **Recommendation:** Rebuild with Quarkus Qute templating engine and JAX-RS resources acting as controllers.
        *   **`index.xhtml` -> `templates/Member/index.html` (Qute template):**
            *   Recreate the form and table structure using Qute syntax.
            *   Form submission will target a JAX-RS endpoint.
        *   **`MemberController.java` -> New methods in `MemberResourceRESTService.java` or a dedicated UI controller JAX-RS resource:**
            *   A JAX-RS resource method will handle rendering the initial page with the member list and an empty form bean (a simple POJO).
            *   Another JAX-RS resource method (`@POST`) will handle form submission (e.g., as `application/x-www-form-urlencoded`). This method will call the `MemberRegistration` service.
            *   Displaying success/error messages: Can be done by passing messages to the Qute template upon re-rendering or using client-side scripting/redirects with query parameters.
            *   The `newMember` concept produced for JSF binding will become a POJO passed to the Qute template.
*   **Like-for-Like UI:** This requires careful re-implementation of the HTML structure and CSS. The existing `screen.css` could be reused if paths are managed correctly.
*   **State Management:** JSF's view state management will be gone. For this simple application, it's likely not an issue. Form re-population on error needs to be handled by passing the submitted (invalid) data back to the template.

### 4.6. Utility / Cross-Cutting Concerns (`util/Resources.java`)

*   **Current:** CDI Producer for `EntityManager` and `Logger`.
*   **Target:**
    *   `EntityManager` producer is obsolete.
    *   `Logger` production: Quarkus has built-in support for injecting `org.jboss.logging.Logger` directly, or standard `java.util.logging.Logger` if preferred. A custom producer is likely unnecessary.
        ```java
        // Example in a Quarkus bean
        import org.jboss.logging.Logger;
        // ...
        private static final Logger LOG = Logger.getLogger(MyService.class);
        // or @Inject Logger log;
        ```

## 5. Configuration Migration

*   **`persistence.xml`:** Obsolete. MongoDB connection details go into `application.properties`.
    *   Example `application.properties` for Quarkus MongoDB:
        ```properties
        quarkus.mongodb.connection-string = mongodb://localhost:27017
        quarkus.mongodb.database = kitchensinkDB
        # For creating unique index on Member email
        # This can be done programmatically or via a migration tool like Mongock/Flyway for MongoDB
        ```
*   **`kitchensink-quickstart-ds.xml`:** Obsolete.
*   **`beans.xml`:** Obsolete. Quarkus scans for beans automatically.
*   **`faces-config.xml`:** Obsolete.
*   **`import.sql` (Data Seeding):**
    *   Implement using a Quarkus `@Startup` bean that runs on application start.
    *   This bean can inject the `MemberRepository` (or use Panache Active Record) to insert the initial "John Smith" member if no members exist.
    *   Alternatively, for more complex data seeding or schema migrations (like index creation), tools like Mongock can be integrated.
    ```java
    // Example Startup Bean for data seeding
    import jakarta.enterprise.context.ApplicationScoped;
    import jakarta.enterprise.event.Observes;
    import io.quarkus.runtime.StartupEvent;
    import org.jboss.as.quickstarts.kitchensink.model.Member; // Adapted Member class

    @ApplicationScoped
    public class DataSeeder {
        void onStart(@Observes StartupEvent ev) {
            if (Member.count() == 0) {
                Member john = new Member();
                john.setName("John Smith");
                john.setEmail("john.smith@mailinator.com");
                john.setPhoneNumber("2125551212");
                john.persist(); // Or use repository
            }
            // Ensure unique index on email - could also be here, or managed externally
            // Member.mongoCollection().createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
        }
    }
    ```

## 6. Build and Deployment Migration

*   **`pom.xml`:**
    *   Change parent to `quarkus-universe-bom` or manage Quarkus dependencies explicitly.
    *   Add Quarkus dependencies: `quarkus-resteasy-reactive-jackson` (for JAX-RS JSON), `quarkus-mongodb-panache`, `quarkus-qute`, `quarkus-bean-validation`, etc.
    *   Remove JBoss EAP, JSF, EJB, JPA-related dependencies.
    *   Update build plugins for Quarkus (e.g., `quarkus-maven-plugin`).
*   **`Dockerfile`:**
    *   Base image should be a JDK 21 image suitable for Quarkus (e.g., `ubi-minimal` with JDK, or a standard OpenJDK image).
    *   Build process will use `mvn package -Dquarkus.package.type=uber-jar` (or `native` for GraalVM).
    *   The `add-user.sh` for WildFly is irrelevant. Quarkus management endpoints are configured differently if needed.
    *   Expose port 8080 (Quarkus default).
*   **`app/charts/helm.yaml`:** Update to reflect new Docker image name, deployment strategy for Quarkus.

## 7. Testing Strategy

*   **Unit Tests:**
    *   Adapt existing JUnit tests.
    *   Mockito can still be used for mocking dependencies.
    *   For Panache/MongoDB interactions, mocking the repository or static Panache methods might be needed.
*   **Integration Tests (`@QuarkusTest`):**
    *   Quarkus provides excellent testing support. `@QuarkusTest` will start the application.
    *   Use REST Assured (comes with Quarkus testing) to test JAX-RS endpoints, ensuring API contracts are met.
    *   **MongoDB for Tests:** Use Testcontainers to spin up a MongoDB instance for integration tests. Quarkus has a `quarkus-test-mongodb` extension that can simplify this (Dev Services for MongoDB).
        ```java
        // Example in a @QuarkusTest
        @Test
        void testListAllMembersEndpoint() {
            given()
              .when().get("/rest/members")
              .then()
                 .statusCode(200)
                 .body("size()", is(1)); // Assuming one seeded member
        }
        ```
*   **Acceptance Tests (`acceptance-tests/`, `ui-acceptance-tests/`):**
    *   These are crucial for verifying "like-for-like".
    *   The `acceptance-tests` (presumably API-level) should pass with minimal changes if the REST API contract is preserved.
    *   The `ui-acceptance-tests` (e.g., Selenium-based) will likely require significant updates due to the UI technology change (JSF to Qute/HTML). Locators and page interaction logic will change.
    *   Configure these tests to run against the Quarkus application, with MongoDB running in a container on the same Docker network if necessary for the test environment.

## 8. Migration Sequence (Step-by-Step Plan)

**Important Note on Version Control:** Each major step and sub-step completed within the phases below should be committed to Git and pushed to the remote repository (e.g., the `feature/quarkus-mongodb-migration` branch). This ensures a granular history, facilitates rollbacks if a particular step introduces unforeseen issues, and allows for collaborative review if multiple developers are involved.

1.  **Phase 0: Preparation & Setup**
    1.  **Baseline:** Ensure all existing tests for the current application pass. Document current API behavior thoroughly (e.g., using Postman collections or OpenAPI specs if available, otherwise derive from tests/code).
    2.  **Project Scaffold:** Create a new Quarkus project using the Quarkus CLI or Maven archetype.
    3.  **JDK Setup:** Configure project for JDK 21.
    4.  **Version Control:** Create a new feature branch for the migration (e.g., `feature/quarkus-mongodb-migration`).
    5.  **Code Formatting:** Setup auto-formatting (e.g., Google Java Format via Spotless Maven plugin).
    6.  **Core Dependencies:** Add initial Quarkus dependencies to `pom.xml` (`quarkus-core`, `quarkus-arc` for CDI, `quarkus-resteasy-reactive-jackson`, `quarkus-mongodb-panache`, `quarkus-qute`, `quarkus-hibernate-validator`).

2.  **Phase 1: Domain Model and Basic Persistence**
    1.  Migrate `model/Member.java` to a Panache Mongo Entity.
    2.  Implement basic CRUD operations using Panache Active Record or Repository for `Member`.
    3.  Set up MongoDB Dev Service in Quarkus for local development and initial testing (Quarkus automatically starts/stops MongoDB).
    4.  Write simple unit/integration tests for Member persistence.
    5.  Implement data seeding for the "John Smith" member.
    6.  Configure unique index on `email` field for MongoDB.

3.  **Phase 2: REST API Layer**
    1.  Migrate `MemberResourceRESTService.java` to Quarkus JAX-RS.
    2.  Adapt service calls to use the new Panache-based data access.
    3.  Adapt validation logic, including the email uniqueness check.
    4.  Write extensive integration tests for all REST endpoints, comparing behavior against the baseline.

4.  **Phase 3: Service Layer**
    1.  Migrate `MemberRegistration.java` EJB to a Quarkus CDI bean.
    2.  Update its persistence logic to use Panache.
    3.  Ensure CDI event firing for `Member` registration is functional.

5.  **Phase 4: Presentation Layer (UI)**
    1.  Design and implement Qute templates for member registration and listing (`index.html`).
    2.  Develop JAX-RS resource methods in `MemberResourceRESTService` (or a new UI-specific resource) to:
        *   Serve the main page with the member list and form.
        *   Handle form submissions (`POST` requests).
    3.  Integrate with `MemberRegistration` service for form submissions.
    4.  Implement display of success/error messages.
    5.  Re-implement the member list display, fetching data via the adapted `MemberListProducer` logic (now likely part of the JAX-RS resource or a dedicated UI service).
    6.  Adapt or rewrite UI acceptance tests.

6.  **Phase 5: Configuration and Utilities**
    1.  Migrate all necessary configurations to `application.properties`.
    2.  Remove obsolete XML configuration files.
    3.  Adapt `util/Resources.java` or remove if not needed (e.g., for Logger).

7.  **Phase 6: Build, Deployment, and Testing Infrastructure**
    1.  Update `Dockerfile` for Quarkus.
    2.  Update `pom.xml` completely for Quarkus build.
    3.  Set up Testcontainers for MongoDB in integration tests if Quarkus Dev Services are not sufficient for CI or specific scenarios.
    4.  Ensure all `acceptance-tests` and `ui-acceptance-tests` can run against the new containerized application, including setting up MongoDB in a container on a shared Docker network for these tests.

8.  **Phase 7: Final Testing and Refinement**
    1.  Execute all tests (unit, integration, acceptance).
    2.  Perform manual exploratory testing of UI and API.
    3.  Code review of all migrated components.
    4.  Performance sanity checks.
    5.  Ensure adherence to SOLID, DRY, Clean Architecture principles. Apply `final` where appropriate.

## 9. Pre and Post Migration Work

### Pre-Migration:
1.  **Detailed Audit of Existing Application:** `ORIGINAL_ARCHITECTURE.md` is a good start. Supplement with API contract documentation (e.g., generate OpenAPI spec from JAX-RS if possible, or manually create one).
2.  **Comprehensive Test Suite for Original App:** Ensure high test coverage for the current JBoss app. These tests serve as the "golden" reference.
3.  **Team Training:** If the team is new to Quarkus or MongoDB, allocate time for learning.
4.  **Environment Setup:** Local dev environments, CI/CD pipeline for the new project.
5.  **Tooling Setup:** Code formatter, linters, IDE configurations.

### Post-Migration:
1.  **Full Regression Testing:** Execute all test suites (unit, integration, all acceptance tests).
2.  **Performance Testing:** Compare against baseline if performance is critical. Quarkus should generally offer improvements.
3.  **Security Review:** Assess new stack for security considerations.
4.  **Documentation:** Update all relevant project documentation (READMEs, deployment guides).
5.  **Monitoring and Logging:** Ensure logging is effective and set up monitoring if deploying to a managed environment.
6.  **Knowledge Transfer:** Ensure team members understand the new architecture.

## 10. Risk Mitigation, Quality Assurance, and Engineering Principles

### Risk Mitigation:
1.  **Incremental Migration (Layer by Layer):** As outlined in the sequence. Allows for focused testing and easier rollback of specific parts if major issues arise.
2.  **Feature Parity Checklists:** Maintain a checklist of all features, API endpoints, and UI interactions to ensure everything is covered.
3.  **Strong Baseline Tests:** Having reliable tests for the original application is critical to verify the "like-for-like" requirement.
4.  **UI Migration Complexity:** The JSF to Qute migration is the highest risk for "like-for-like" UI. Allocate sufficient time and potentially use UI comparison tools if pixel-perfect is required (though functional equivalence is usually the primary goal).
5.  **Data Consistency with MongoDB:** Ensure unique constraints (like email) are properly implemented in MongoDB.
6.  **Dependency Management:** Carefully manage Quarkus extensions and their compatibility.

### Quality Assurance:
1.  **Code Reviews:** Mandatory for all migrated code. Focus on correctness, adherence to principles, and test coverage.
2.  **Automated Testing:** Unit, integration, and acceptance tests integrated into CI.
3.  **Static Code Analysis:** Integrate tools for identifying potential bugs, code smells (e.g., SonarQube, or linters provided by IDEs/plugins).
4.  **Pair Programming:** For complex or critical sections of the migration.

### Software Engineering Principles:
*   **SOLID:**
    *   *Single Responsibility Principle:* Quarkus beans (services, resources) should have focused responsibilities.
    *   *Open/Closed Principle:* Design components to be extensible (e.g., new features) but closed for modification of core behavior.
    *   *Liskov Substitution Principle:* Not directly applicable here as we are not heavily using inheritance for polymorphism in this app, but interfaces for services could be used if complexity grew.
    *   *Interface Segregation Principle:* Define focused interfaces if used (e.g., for services if they become complex).
    *   *Dependency Inversion Principle:* Quarkus CDI promotes this heavily. Depend on abstractions.
*   **DRY (Don't Repeat Yourself):**
    *   Identify and refactor any duplicated logic from the original application.
    *   Use utility classes or shared services for common tasks.
*   **LEAN Architecture:**
    *   Focus on delivering functional equivalence efficiently.
    *   Avoid over-engineering. Quarkus philosophy aligns well with this.
    *   Eliminate unnecessary configurations and boilerplate (Quarkus helps here).
*   **Clean Architecture:**
    *   Maintain separation between layers: Domain (Panache Entities), Application (Services, JAX-RS Resources handling application logic), Infrastructure (Panache Repositories, Qute templates).
    *   Domain logic should be independent of framework specifics as much as possible (Panache Active Record blurs this slightly, but the core entity fields and validation remain domain-focused).
*   **Immutability and `final`:**
    *   Use `final` for variables whose values should not change after initialization (local variables, method parameters, fields where appropriate).
    *   Consider records for DTOs or simple data carriers to promote immutability. Panache entities are typically mutable.
*   **Auto-Formatting:**
    *   Implement a Maven plugin (e.g., Spotless with Google Java Format) to enforce consistent code style across the team and in CI.
    ```xml
    <!-- Example Spotless Configuration in pom.xml -->
    <plugin>
        <groupId>com.diffplug.spotless</groupId>
        <artifactId>spotless-maven-plugin</artifactId>
        <version>2.43.0</version> <!-- Use latest version -->
        <configuration>
            <java>
                <googleJavaFormat>
                    <version>1.17.0</version> <!-- Use latest version -->
                    <style>AOSP</style> <!-- or GOOGLE -->
                </googleJavaFormat>
                <removeUnusedImports/>
            </java>
        </configuration>
        <executions>
            <execution>
                <goals>
                    <goal>check</goal> <!-- Use 'apply' to auto-format -->
                </goals>
            </execution>
        </executions>
    </plugin>
    ```

## 11. Insights for the AI (Self-Guidance for Migration)

*   **Focus on API Contract First:** The REST API is a critical external contract. Prioritize getting this layer functionally identical, backed by robust integration tests.
*   **UI is the Trickiest "Like-for-Like":** Be meticulous in replicating UI structure, form fields, and displayed data. `ui-acceptance-tests` will be your best friend here. Use browser developer tools to compare original and migrated HTML/CSS if visual discrepancies arise.
*   **Test Incrementally:** After migrating each small piece (e.g., a single repository method, a REST endpoint), test it immediately. Don't wait for an entire layer to be complete.
*   **MongoDB Schema Design:** For `Member`, it's simple. For more complex entities in other projects, careful thought on embedding vs. referencing would be needed. Ensure the unique index for `email` is created reliably (e.g., via a startup bean or migration tool).
*   **Quarkus Dev Mode is Powerful:** Leverage `quarkus:dev` for live reloading during development of both backend logic and Qute templates.
*   **Error Handling Parity:** Pay close attention to replicating error messages and status codes from the original API, especially validation errors.
*   **Eventing System:** The CDI event `Event<Member>` and its observer `onMemberListChanged` will need careful porting. Ensure the new UI layer correctly reflects data changes post-registration, similar to how the original `MemberListProducer` updated the JSF view. This might involve Qute template re-rendering logic or a more reactive approach if desired.
*   **Consult Quarkus Guides:** The official Quarkus guides are excellent. Refer to them for MongoDB, RESTEasy Reactive, Qute, Testing, etc.
*   **Small Application Advantage:** The Kitchensink app is relatively small. This makes a full rewrite of layers feasible and less risky than for a large enterprise application. The key is disciplined testing. 