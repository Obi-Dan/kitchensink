# Original Architecture of the Kitchensink Application

This document outlines the architecture of the Kitchensink application found within the `app` directory.

## 1. Overview

The Kitchensink application is a Jakarta EE web application designed as a starter project for JBoss Enterprise Application Platform (EAP) / WildFly. It demonstrates common Jakarta EE features and best practices for building enterprise applications.

**Core Technologies:**

*   **Jakarta EE 8/9 (based on dependencies)**:
    *   **CDI (Contexts and Dependency Injection)**: For dependency management and bean lifecycle.
    *   **JSF (Jakarta Server Faces)**: For the web user interface (UI layer).
    *   **JPA (Jakarta Persistence API)**: For object-relational mapping (ORM) to interact with a database (Persistence Layer). Hibernate is the underlying provider.
    *   **EJB (Enterprise JavaBeans)**: For transactional business logic (Service Layer).
    *   **JAX-RS (Jakarta RESTful Web Services)**: For exposing REST APIs.
    *   **Bean Validation**: For data validation using annotations.
*   **Maven**: For project build and dependency management.
*   **H2 Database**: Used as an in-memory database for development and testing.
*   **WildFly/JBoss EAP**: The target application server.
*   **Docker**: For containerization of the application.
*   **Helm**: (Indicated by `app/charts/helm.yaml`) For Kubernetes deployment.

**Packaging:** The application is packaged as a WAR (Web Application Archive) file named `kitchensink.war`.

## 2. Project Structure (within `app` directory)

The `app` directory follows a standard Maven project structure:

*   `pom.xml`: Maven Project Object Model, defines dependencies, build process, and project metadata.
*   `Dockerfile`: Defines how to build a Docker image for the application, deploying it on a WildFly base image.
*   `.dockerignore`: Specifies files to exclude from the Docker build context.
*   `charts/`: Contains Helm chart configurations (`helm.yaml`) for Kubernetes deployment.
*   `src/`:
    *   `main/`: Contains the application's source code and resources.
        *   `java/org/jboss/as/quickstarts/kitchensink/`: Java source code, organized by layer/responsibility.
            *   `controller/`: JSF backing beans.
            *   `data/`: Data access layer components (repositories, producers).
            *   `model/`: JPA entity classes.
            *   `rest/`: JAX-RS resource classes.
            *   `service/`: EJB service beans.
            *   `util/`: Utility classes (e.g., CDI producers for resources).
        *   `resources/`: Application resources.
            *   `META-INF/persistence.xml`: JPA configuration.
            *   `import.sql`: SQL script for initial data seeding.
        *   `webapp/`: Web application content.
            *   `index.xhtml`: Main JSF page for user interaction.
            *   `index.html`: Redirects to `index.jsf`.
            *   `WEB-INF/`: Web application configuration.
                *   `beans.xml`: CDI configuration (enables CDI).
                *   `faces-config.xml`: JSF configuration.
                *   `kitchensink-quickstart-ds.xml`: WildFly data source definition for H2.
                *   `templates/default.xhtml`: JSF Facelets template.
            *   `resources/`: JSF web resources.
                *   `css/screen.css`: Stylesheet for the application.
                *   `gfx/`: Image files.
    *   `test/`: Contains unit and integration tests (primarily using JUnit, Arquillian, and Mockito).

*   `target/`: (Ignored as per `.gitignore`) Contains compiled code and packaged artifacts.

## 3. Layers and Responsibilities

The application can be broken down into the following logical layers:

### 3.1. Presentation Layer (User Interface)

*   **Technology**: Jakarta Server Faces (JSF) with Facelets templating.
*   **Components**:
    *   `index.xhtml`: The main view, providing a form for member registration and a table to display registered members. It uses Expression Language (EL) to bind to backend beans and display data.
    *   `WEB-INF/templates/default.xhtml`: A common Facelets template for page layout and structure.
    *   `resources/css/screen.css`: CSS for styling the application.
    *   `resources/gfx/`: Images used in the UI.
*   **Controller (JSF Backing Beans)**:
    *   `controller/MemberController.java`: A CDI bean (`@Model`) that handles UI logic for member registration.
        *   Exposes a `newMember` object (instance of `model.Member`) to the JSF page for form binding.
        *   Provides the `register()` action method, which interacts with the service layer (`MemberRegistration`) to save new members.
        *   Uses `FacesContext` to display messages (success/error) to the user.

### 3.2. REST API Layer

*   **Technology**: Jakarta RESTful Web Services (JAX-RS).
*   **Components**:
    *   `rest/JaxRsActivator.java`: An `Application` subclass that activates JAX-RS and defines the base path `/rest`.
    *   `rest/MemberResourceRESTService.java`: A JAX-RS resource class mapped to `/members` (full path `/rest/members`).
        *   `GET /rest/members`: Returns a list of all members in JSON format.
        *   `GET /rest/members/{id}`: Returns a specific member by ID in JSON format.
        *   `POST /rest/members`: Creates a new member from JSON data. Performs validation (Bean Validation and custom email uniqueness check) and uses the `service.MemberRegistration` EJB to persist the member. Returns appropriate HTTP responses (200 OK, 400 Bad Request for validation errors, 409 Conflict for duplicate email).
*   **Interaction**: This layer interacts with the Data Access Layer (`MemberRepository`) for read operations and the Service Layer (`MemberRegistration`) for write operations.

### 3.3. Service Layer (Business Logic)

*   **Technology**: Enterprise JavaBeans (EJB - Stateless Session Beans).
*   **Components**:
    *   `service/MemberRegistration.java`: A `@Stateless` EJB responsible for the business logic of registering a new member.
        *   Injects an `EntityManager` for database persistence.
        *   Injects a CDI `Event<Member>` source.
        *   The `register(Member member)` method persists the `Member` entity and then fires a CDI event with the newly registered `Member`. This event is observed by other components (e.g., `MemberListProducer`) to react to data changes.

### 3.4. Data Access Layer (DAL)

*   **Technology**: Jakarta Persistence API (JPA) with Hibernate as the provider, and CDI for managing repositories/producers.
*   **Components**:
    *   **Repository**:
        *   `data/MemberRepository.java`: An `@ApplicationScoped` CDI bean that encapsulates data access logic for `Member` entities.
            *   Injects an `EntityManager`.
            *   Provides methods like `findById()`, `findByEmail()`, and `findAllOrderedByName()` using JPA Criteria API.
    *   **Data Producer**:
        *   `data/MemberListProducer.java`: A `@RequestScoped` CDI bean.
            *   Injects `MemberRepository` to fetch member data.
            *   Uses `@Produces` and `@Named("members")` to make the list of all members (ordered by name) available to the JSF UI via EL (`#{members}`).
            *   Observes the CDI event fired by `MemberRegistration` (when a new member is registered) to refresh its list of members (`onMemberListChanged()`).
            *   Initializes the member list on creation (`@PostConstruct`).

### 3.5. Domain Model (Entities)

*   **Technology**: JPA Entities.
*   **Components**:
    *   `model/Member.java`: A JPA `@Entity` class representing a member.
        *   Fields: `id` (primary key, auto-generated), `name`, `email` (unique), `phoneNumber`.
        *   Includes Bean Validation annotations (`@NotNull`, `@Size`, `@Pattern`, `@Email`, `@Digits`) to define constraints on its properties.
        *   Annotated with `@XmlRootElement` for JAX-RS marshalling (XML/JSON).

### 3.6. Utility / Cross-Cutting Concerns

*   **Technology**: CDI Producers.
*   **Components**:
    *   `util/Resources.java`: A CDI utility class that produces resources for injection.
        *   Produces an `EntityManager` via `@Produces` and `@PersistenceContext`, making it available for injection across the application.
        *   Produces a `java.util.logging.Logger` configured for the injection point's class.

## 4. Configuration

### 4.1. Persistence Configuration

*   `src/main/resources/META-INF/persistence.xml`:
    *   Defines a persistence unit named "primary".
    *   Configures the JTA data source `java:jboss/datasources/KitchensinkQuickstartDS`.
    *   Sets Hibernate properties:
        *   `hibernate.hbm2ddl.auto="create-drop"`: Schema is created and dropped on each deployment (development mode).
        *   `hibernate.show_sql="false"`.
*   `src/main/webapp/WEB-INF/kitchensink-quickstart-ds.xml`:
    *   Defines the `KitchensinkQuickstartDS` data source for WildFly.
    *   Configures it to use an H2 in-memory database (`jdbc:h2:mem:kitchensink-quickstart`).
    *   Specifies `sa`/`sa` as credentials. This file is deployed with the WAR and provides the database connection for the application.

### 4.2. CDI Configuration

*   `src/main/webapp/WEB-INF/beans.xml`:
    *   Enables CDI with `bean-discovery-mode="all"`, allowing the container to scan all classes for beans.

### 4.3. JSF Configuration

*   `src/main/webapp/WEB-INF/faces-config.xml`:
    *   Basic JSF configuration file (version 4.0). Activates the JSF servlet. No custom navigation rules or managed beans are defined here, as CDI and annotations are preferred.

### 4.4. Data Seeding

*   `src/main/resources/import.sql`:
    *   Contains SQL statements to populate the database with initial data (one sample member) when the application starts, due to `hibernate.hbm2ddl.auto` being active.

## 5. Build and Deployment

*   **Build**: Maven is used to compile, test, and package the application into a WAR file.
*   **Local Deployment**: The WAR can be deployed to a JBoss EAP or WildFly server.
*   **Containerization**:
    *   `app/Dockerfile`: Provides instructions to build a Docker image. It uses a multi-stage build:
        1.  A Maven builder image compiles the application.
        2.  The resulting WAR is copied into a WildFly base image.
    *   The Docker image exposes application (8080) and management (9990) ports.
    *   An admin user for the WildFly console is created.
*   **Kubernetes/OpenShift**:
    *   `app/charts/helm.yaml`: Suggests support for Helm-based deployment, possibly on OpenShift, referencing the Git repository for the build source.
    *   The `pom.xml` includes an "openshift" profile that configures the `eap-maven-plugin` to package the application for OpenShift using Galleon layers.

## 6. Key Data Flows

### 6.1. Member Registration (UI)

1.  User fills the registration form in `index.xhtml`.
2.  JSF binds form data to `MemberController.newMember`.
3.  User clicks "Register".
4.  `MemberController.register()` method is invoked.
5.  `MemberController` calls `MemberRegistration.register(newMember)`.
6.  `MemberRegistration` (EJB) persists the `Member` entity using `EntityManager`.
7.  `MemberRegistration` fires a CDI event (`@Observes Member member`).
8.  `MemberListProducer.onMemberListChanged()` observes the event and calls `retrieveAllMembersOrderedByName()` to refresh its internal list.
9.  `MemberController` adds a `FacesMessage` (success/error) to `FacesContext`.
10. JSF re-renders `index.xhtml`; the updated member list (from `#{members}` via `MemberListProducer`) and messages are displayed.

### 6.2. Member Registration (REST API)

1.  Client sends a `POST` request with member JSON data to `/rest/members`.
2.  `MemberResourceRESTService.createMember(Member member)` is invoked.
3.  `validateMember()` is called:
    *   Bean Validation annotations on `Member` are checked using injected `Validator`.
    *   `emailAlreadyExists()` (which uses `MemberRepository.findByEmail()`) checks for email uniqueness.
4.  If validation fails, an appropriate error `Response` (400 or 409) is returned.
5.  If validation passes, `MemberRegistration.register(member)` is called.
6.  (Same as steps 6-7 in UI flow) `MemberRegistration` persists the member and fires a CDI event.
7.  An HTTP 200 OK `Response` is returned.

### 6.3. Viewing Members (UI)

1.  User navigates to `index.xhtml`.
2.  JSF requests the `#{members}` list.
3.  `MemberListProducer.getMembers()` is invoked, returning the cached list of members.
4.  If the list is initially populated or refreshed, `MemberListProducer.retrieveAllMembersOrderedByName()` calls `MemberRepository.findAllOrderedByName()` to fetch data from the database.
5.  The `<h:dataTable>` in `index.xhtml` displays the members.

### 6.4. Viewing Members (REST API)

1.  Client sends a `GET` request to `/rest/members` or `/rest/members/{id}`.
2.  `MemberResourceRESTService.listAllMembers()` or `lookupMemberById(id)` is invoked.
3.  The method calls `MemberRepository.findAllOrderedByName()` or `findById(id)` respectively.
4.  The `Member` data is returned as JSON.

This comprehensive examination covers the structure, technologies, layers, and key functionalities of the Kitchensink application within the `app` directory. 