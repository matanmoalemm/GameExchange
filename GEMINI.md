# Gemini Project Instructions: Game Exchange

This document provides foundational mandates and architectural guidance for the Game Exchange project. Adhere to these instructions to ensure consistency, security, and high engineering standards.

## Project Overview
Game Exchange is a RESTful backend for a game marketplace platform. Users can list, browse, and comment on video games.
- **Core Technologies:** Java 21, Spring Boot 4.0.6, PostgreSQL.
- **Security:** Stateless authentication using Google OAuth2 (OIDC) and JWT (jjwt 0.12.3).
- **Architecture:** Layered architecture (Controller -> Service -> Repository) with DTOs (Java Records) for API communication.
- **Mapping:** MapStruct 1.6.3 for entity-DTO transformations.

## Building and Running
- **Build:** `./mvnw clean install`
- **Run:** `./mvnw spring-boot:run`
- **Test:** `./mvnw test`
- **Infrastructure:** Use `docker-compose.yml` to spin up the required PostgreSQL instance.

## Development Conventions

### Security & Authentication
- **Statelessness:** The server is stateless; no HTTP sessions are used.
- **JWT Filter:** `JwtAuthenticationFilter` validates the `Authorization: Bearer <token>` header on every request.
- **Current User:** Access the authenticated user in controllers via `@AuthenticationPrincipal UserPrincipal`.
- **Ownership:** Service layer MUST enforce ownership for mutating operations (UPDATE/DELETE). Throw `403 Forbidden` on mismatch.

### Data Access & Mapping
- **DTOs:** Use Java Records for `Request`, `Response`, and `UpdateDTO` objects.
- **Mapping Strategy:** Use MapStruct for all mappings. For partial updates (PATCH), use `NullValuePropertyMappingStrategy.IGNORE` to prevent overwriting existing data with nulls.
- **Validation:** Use Jakarta Bean Validation annotations in DTOs (e.g., `@NotBlank`, `@Size`, `@Min`).

### Error Handling
- **Global Handler:** `ControllerExceptionHandler.java` handles common exceptions and returns consistent error responses.
- **Validation Errors:** Handled automatically; ensure response format matches existing conventions.

## Testing Practices
The project follows a two-tier testing strategy:
1. **Unit Tests:** Located in `src/test/java/com/app/**/ServiceTest.java`. Use Mockito and JUnit 5. No Spring context loading.
2. **Integration Tests:** Located in `src/test/java/com/app/**/IntegrationTest.java`.
   - Inherit from `BaseIntegrationTest`.
   - Use a real PostgreSQL database (provided via Docker or local setup).
   - Verify the full HTTP stack using `MockMvc` or `RestTestClient`.
   - Ensure clean state before each test by deleting data in reverse FK order.

## Project Structure
- `com.app.Security`: Authentication logic, JWT utilities, and Security configuration.
- `com.app.User`: User entity, service, repository, and DTOs.
- `com.app.Post`: Listing/Post domain logic.
- `com.app.Comment`: Commenting system logic.
- `com.app.ControllerExceptionHandler`: Global REST API error management.

---
*Note: This file is a foundational mandate for the Gemini CLI agent. Do not modify the core architectural directions without explicit user instruction.*
