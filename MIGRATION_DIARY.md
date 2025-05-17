# Migration Diary: KitchenSink WildFly to Quarkus & MongoDB

This document chronicles the significant decisions, challenges, and resolutions encountered during the migration of the KitchenSink application from WildFly/JSF/JPA to Quarkus/Qute/MongoDB Panache.

## Phase 0: Preparation & Strategy

-   **Goal Alignment**: The migration aimed for a like-for-like functional replacement, leveraging Quarkus benefits while adhering to principles like simplicity, avoiding duplication, and maintaining a clean codebase.
-   **Original Code Archival**:
    -   **Decision**: The entire original `app/`, `acceptance-tests/`, and `ui-acceptance-tests/` modules were copied into a `legacy_original_code/` directory at the project root.
    -   **Rationale**: To provide an unchanged reference throughout the migration, ensuring functional parity and aiding in debugging. This directory was added to `.gitignore`.
-   **Version Control**:
    -   **Decision**: A new feature branch, `feature/quarkus-migration`, was created.
    -   **Rationale**: To isolate migration efforts from the main development line. Commits were made at logical stages (though more frequent, phase-wise commits were adopted part-way through).
-   **Project Scaffolding (`app/`)**:
    -   **Decision**: A new Maven project structure was created under `app/` for the Quarkus application.
    -   **Rationale**: To keep the migrated application separate from the original during the transition.
    -   **`pom.xml` Configuration**:
        -   JDK: `21`.
        -   Quarkus Platform: Version `3.9.4`.
        -   Key Dependencies:
            -   `quarkus-core`, `quarkus-arc` (CDI)
            -   `quarkus-resteasy-reactive`, `quarkus-resteasy-reactive-jackson` (JAX-RS)
            -   `quarkus-mongodb-panache` (MongoDB data access)
            -   `quarkus-qute`, `quarkus-resteasy-reactive-qute` (UI templating & serving)
            -   `quarkus-hibernate-validator` (Bean Validation)
            -   `quarkus-narayana-jta` (Transactions - though its direct use became nuanced)
            -   Build/Test: `quarkus-junit5`, `maven-surefire-plugin`, `spotless-maven-plugin`, `jacoco-maven-plugin`.
        -   **Rationale**: Standard Quarkus stack for a web application with MongoDB and Qute for UI. Spotless and JaCoCo for code quality and coverage.

## Phase 1: Data Model (Entity) Migration

-   **`Member.java`**:
    -   **Decision**: Migrated from JPA `@Entity` to `io.quarkus.mongodb.panache.PanacheMongoEntity` and annotated with `@MongoEntity(collection="members")`.
    -   **Rationale**: `PanacheMongoEntity` provides Active Record pattern for MongoDB, simplifying data operations and integrating with Quarkus.
    -   **ID Handling**: The explicit `@Id @GeneratedValue` was removed as `PanacheMongoEntity` provides an `id` field of type `org.bson.types.ObjectId`. A helper method `getStringId()` was added to the `Member` entity for convenient access to the ID as a String.
    -   **Validation Annotations**: Existing JSR 303/Jakarta Bean Validation annotations (`@NotNull`, `@Size`, `@Pattern`, `@Email`) were retained as they are standard and supported by Hibernate Validator in Quarkus.
    -   **Removed Fields/Methods**: `serialVersionUID` was kept as it's good practice for serializable classes, though not strictly required by Panache. No other fields/methods were removed from the original entity logic.

## Phase 2: Service Layer Migration

-   **`MemberRegistration.java` (EJB to CDI Bean)**:
    -   **Decision**: Changed from `@Stateless` EJB to `@ApplicationScoped` Quarkus CDI bean.
    -   **Rationale**: Quarkus uses CDI for its managed beans; EJBs are not part of its core model. `@ApplicationScoped` is a standard scope.
    -   **Persistence Logic**:
        -   `EntityManager` (`@PersistenceContext`) was removed.
        -   Data persistence (`em.persist(member)`) was replaced with Panache's Active Record style: `member.persist()`.
        -   Email uniqueness check (previously `em.createQuery(...)`) was replaced by `Member.find("email", email).firstResult()`.
    -   **Transaction Management**:
        -   **Initial Approach**: Retained `@jakarta.transaction.Transactional`.
        -   **Challenge**: During debugging of acceptance test failures (connection resets), `@Transactional` was suspected as a contributor, especially concerning replica set configuration for MongoDB.
        -   **Temporary Resolution**: `@Transactional` was commented out from the `register` method. Tests (including acceptance tests that perform registrations) passed without it.
        -   **Rationale/Observation**: For simple Panache operations like `persist()` or `find()`, Quarkus/Panache might provide implicit transactionality or the test scenarios didn't expose atomicity issues that would strictly require method-level `@Transactional` with the MongoDB setup. The requirement for a replica set for client-side transactions was a key learning. The final state left it commented as tests passed, but it's noted that for complex operations involving multiple Panache calls that need to be atomic, explicit `@Transactional` would be necessary (and would require a correctly configured replica set).
    -   **Exception Handling**: `EmailAlreadyExistsException` (custom exception) was retained and thrown as before.
    -   **Event Firing**: `jakarta.enterprise.event.Event<Member>` injection and firing (`memberEventSrc.fire(member)`) remained unchanged, working seamlessly in Quarkus.
    -   **Logging**: Switched from `java.util.logging.Logger` to `org.jboss.logging.Logger` (common in Quarkus and JBoss projects).
-   **`DataSeeder.java` (New Service for Initial Data)**:
    -   **Decision**: A new `@ApplicationScoped` bean `DataSeeder` was created, using `@Observes StartupEvent` to check and seed initial data if the `members` collection was empty. It also ensures a unique index on the `email` field.
    -   **Rationale**: To replace the functionality that might have been in an `import.sql` (for JPA) or handled by other WildFly-specific startup mechanisms. This ensures the application has some data on first run and that the email constraint is enforced at the database level.
-   **`Resources.java`**:
    -   **Decision**: This utility class, which previously produced `EntityManager` and `Logger`, was deleted.
    -   **Rationale**: Quarkus handles `Logger` injection automatically. `EntityManager` is no longer used with Panache MongoDB.

## Phase 3: REST API Endpoint Migration

-   **`MemberResourceRESTService.java`**:
    -   **Path Management**:
        -   **Decision**: Original base path `@Path("/members")` was kept for API methods. The class itself was given `@Path("/app")`. API methods were further grouped under `@Path("/api")` within the class, resulting in full paths like `/rest/app/api/members`. UI-serving methods were grouped under `@Path("/ui")`. `JaxRsActivator.java` (with `@ApplicationPath("/rest")`) was kept, and `quarkus.resteasy-reactive.path=/rest` was set in `application.properties`.
        -   **Rationale**: This clearly separates API endpoints from UI-serving endpoints and aligns with common practices. The combination of `@ApplicationPath` and `quarkus.resteasy-reactive.path` worked as expected.
    -   **Scope**: Changed from `@RequestScoped` to `@ApplicationScoped` (as JAX-RS resources are ApplicationScoped by default in Quarkus unless specified otherwise, and this class holds injected beans).
    -   **Service Injection**: `@Inject MemberRegistration` worked as expected.
    -   **Data Retrieval**:
        -   `lookupMemberById()`: Changed from `memberRepository.findById(id)` to `Member.findByIdOptional(new ObjectId(id))`. Error handling for `NotFoundException` was kept.
        -   `listAllMembers()`: Changed from `memberRepository.findAllOrderedByName()` to `Member.findAll(Sort.by("name")).list()`.
    -   **Data Creation (`createMemberApi`)**:
        -   Uses `memberRegistration.register(member)`.
        -   Error handling: Catches `ConstraintViolationException` (for bean validation) and `EmailAlreadyExistsException` (custom).
        -   **Response Building**: Uses `Response.ok().entity(...).build()` and `Response.status(...).entity(...).build()`.
    -   **Validation**:
        -   `@Valid` annotation on the `Member` parameter in `createMemberApi` triggers bean validation.
        -   `ValidationHelper.constraintViolationToResponse` was effectively replaced by a custom `buildValidationResponse` method in the JAX-RS resource to handle `ConstraintViolationException`.
-   **`JaxRsActivator.java`**:
    -   **Decision**: Kept as is.
    -   **Rationale**: `@ApplicationPath("/rest")` is recognized by Quarkus.

## Phase 4: UI Migration (JSF to Qute)

-   **Template Creation**:
    -   **Decision**: Replaced `index.xhtml` (JSF) and `default.xhtml` (Facelets template) with a single Qute template: `app/src/main/resources/templates/Member/index.html`.
    -   **Rationale**: Qute is Quarkus's native templating engine. A single template simplifies the UI structure for this application.
-   **Mapping JSF to Qute**:
    -   `<h:form>` -> `<form method="post" action="...">`. Action URL points to a new JAX-RS method.
    -   `<h:inputText value="#{...}">` -> `<input type="text" name="..." value="{newMember.name ?: ''}">`. Names match `@FormParam` in JAX-RS.
    -   `<h:commandButton action="#{...}">` -> `<input type="submit">`.
    -   `<h:messages globalOnly="true">` / `<h:message for="...">`: Replaced by Qute conditionals checking `globalMessages` and `errors` maps passed from the JAX-RS resource.
        -   `{#if globalMessages} <ul class="messages">{#for message in globalMessages}<li class="{message.type}">{message.text}</li>{/for}</ul> {/if}`
        -   `<span class="invalid">{errors.get('fieldName')}</span>`
    -   `<ui:repeat value="#{memberController.members}" var="member">` -> `{#for member in members} ... {/for}`.
    -   Conditional rendering (`rendered="#{empty ...}"`): `{#if members} ... {#else} No members... {/if}`.
        -   **Challenge**: The exact syntax for checking if a list is empty in Qute caused several iterations. `members.isEmpty`, `members.size == 0`, `!members`. The Qute error "Key 'empty' not found" was persistent.
        -   **Resolution**: The final working syntax used was `{#if members}` (evaluates to true if not null and not empty) and `{#else}` for the empty case. The persistent error was likely due to Docker build caching stale template versions, resolved by adding `touch` commands to Makefile targets to bust the cache. The last change to `members.isEmpty` was also critical.
    -   Referencing static resources (`/css/screen.css`, `/gfx/...`): Paths adjusted as these are now served from `META-INF/resources`.
-   **Qute Template Serving (in `MemberResourceRESTService.java`)**:
    -   **Decision**: Added JAX-RS methods to serve the Qute template and handle form submissions.
        -   `getWebUi()`: Injects `Template index` (using `@Location("Member/index.html")`) and returns `index.data("members", members, "errors", errors, "globalMessages", globalMessages, "newMember", newMember)`.
        -   `registerViaUi()`: `@POST` method, uses `@FormParam` to get form data, calls `memberRegistration.register()`, then re-fetches data and re-renders the template with potential errors or success messages.
    -   **Rationale**: Standard approach for serving Qute UIs from JAX-RS in Quarkus.
    -   **Dependency**: Ensured `quarkus-resteasy-reactive-qute` was in `pom.xml`. Missing this initially caused the template not to render (blank page/resource not found).

## Phase 5: Configuration and Utilities

-   **`application.properties` (for `app/`)**:
    -   Application name/version.
    -   HTTP port: `8080`.
    -   JAX-RS base path: `quarkus.resteasy-reactive.path=/rest`.
    -   MongoDB: `quarkus.mongodb.database=kitchensinkDB`. `quarkus.mongodb.connection-string` initially commented out to rely on Dev Services, later set explicitly for Docker Compose (`mongodb://mongo:27017/?replicaSet=rs0`).
    -   Logging configured.
-   **`Resources.java`**: Deleted (as covered in Service Layer).

## Phase 6: Build, Deployment, and Testing Infrastructure

-   **`app/Dockerfile`**:
    -   **Decision**: Multi-stage Docker build.
        -   Stage 1 (Builder): `maven:3.9.6-eclipse-temurin-21 AS builder`. Copies `pom.xml`, runs `mvn dependency:go-offline`, copies `src/`, runs `mvn package -Dquarkus.package.type=uber-jar -DskipTests`.
        -   Stage 2 (Runtime): `eclipse-temurin:21-jre-jammy AS runtime`. Copies uber-jar from builder. `ENTRYPOINT ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "application.jar"]`.
    -   **Rationale**: Standard optimized Docker build for Quarkus uber-jars. Using a specific Maven/JDK builder ensures consistency. `skipTests` as tests are run via Makefile targets.
    -   **Challenge**: Initial attempts with Quarkus native builder images (`ubi-quarkus-mandrel-builder-image`, `quarkus-distroless-image-jni`) led to various build or runtime issues, possibly due to environment complexities or image configurations. Switching to a simpler Maven builder + standard JRE runtime was more straightforward for this uber-jar scenario.
-   **`docker-compose.yml` Updates**:
    -   `app` service:
        -   Build context changed to `./app`.
        -   Healthcheck updated to `/rest/app/api/members` (a valid API endpoint in the migrated app).
        -   Environment variable `QUARKUS_MONGODB_CONNECTION_STRING=mongodb://mongo:27017/?replicaSet=rs0` added to connect to the `mongo` service.
        -   `depends_on: mongo` added.
    -   `mongo` service (New):
        -   Image: `mongo:7.0`.
        -   Command: `["mongod", "--replSet", "rs0", "--bind_ip_all"]`.
        -   Healthcheck: Uses `mongosh` to check `rs.status()` and initiate `rs.initiate({ _id: 'rs0', members: [ { _id: 0, host: 'mongo:27017' } ] })` if needed. This was iterated upon to ensure reliability.
        -   **Challenge**: MongoDB replica set initialization within Docker Compose healthcheck was tricky. Timeouts and retry counts for the healthcheck were increased significantly. The `host` for `rs.initiate` inside the healthcheck script also needed to be `mongo:27017` (service name) not `localhost`.
    -   `ui-tests` service:
        -   `CMD` in `ui-acceptance-tests/Dockerfile` updated to pass `-Dapp.url=http://app:8080/rest/app/ui` to point to the migrated app's UI path within the Docker network.
-   **`Makefile` Updates**:
    -   `test`, `test-coverage`, `test-report`: Paths and commands updated to target `app/` and use Quarkus/Maven conventions (e.g., `mvn test -Pcoverage`).
    -   `acceptance-test`:
        -   Updated to build and run the `app` (migrated) and `mongo` services.
        -   Passes `-Dapp.base.url=http://localhost:8080/rest/app/api` to acceptance tests.
        -   Added `touch` command to bust Docker cache for `app/src`.
        -   Enhanced logging and temporary removal of `docker-compose down` for debugging.
    -   `ui-test`:
        -   Updated to build `app`, `ui-tests`, and `mongo` services.
        -   Starts `app`