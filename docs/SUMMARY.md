# Kitchensink: JBoss EAP Quickstart Project Summary

## Project Overview
The "kitchensink" project is a Java Enterprise Application (Jakarta EE 10) quickstart sample for JBoss Enterprise Application Platform (EAP). It demonstrates the integration of multiple Jakarta EE technologies in a single application.

## Featured Technologies
- **JSF (Jakarta Server Faces)** - Web user interface
- **CDI (Contexts and Dependency Injection)** - Dependency management
- **JPA (Jakarta Persistence API)** - Database access and ORM
- **EJB (Enterprise JavaBeans)** - Business logic and transactions
- **JAX-RS (Jakarta RESTful Web Services)** - RESTful API endpoints
- **Bean Validation** - Data validation

## Application Features
- Member registration system with validated form inputs
- Display of registered members in a table view
- REST API for programmatic access to member data
- H2 in-memory database for data storage (development only)

## REST API Endpoints
All endpoints are relative to the base path `/rest`:

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/members` | List all members | 200 OK with JSON array of members |
| GET | `/members/{id}` | Get a specific member by ID | 200 OK with member JSON or 404 Not Found |
| POST | `/members` | Create a new member | 200 OK on success, 400 Bad Request for validation errors, 409 Conflict for duplicate email |

## Project Structure
- Model classes representing the data domain (Member entity)
- Service layer for business logic
- Controllers to handle user interactions
- REST endpoints for API access
- JSF views for the user interface

## Deployment
The application is packaged as a WAR file for deployment on JBoss EAP or WildFly application servers.

## Purpose
This quickstart serves as a learning tool and starting point for developers to understand Jakarta EE technologies and provides a foundation for building more complex enterprise applications. 