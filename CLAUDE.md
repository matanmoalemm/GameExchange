# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=UserControllerFullIntegrationTest

# Run a single test method
./mvnw test -Dtest=UserControllerFullIntegrationTest#shouldCreateUser
```

Tests require a running PostgreSQL instance (see Database Setup below).

## Database Setup

The app connects to PostgreSQL at `localhost:5332` (non-standard port), database `mydata`, user `matan`, password `password`. `spring.jpa.hibernate.ddl-auto=create` means the schema is **dropped and recreated on every startup**.

## Required application.properties entries

`JwtUtils` reads two properties that are not yet in `application.properties` and will cause startup failure if missing:

```properties
app.jwtSecret=<at-least-32-char-secret>
app.jwtExpirationMs=86400000
```

## Architecture

**Domain packages** — each follows the same layered pattern:

| Layer | Files |
|---|---|
| Entity | `Post.java`, `User.java`, `Comment.java` |
| Repository | Spring Data JPA interface |
| Service | Business logic, throws `NoSuchElementException` for missing resources |
| Controller | `@RestController`, delegates entirely to service |
| DTO | Request/Response Java records with Bean Validation annotations |
| Mapper | MapStruct interface (`componentModel = "spring"`, `NullValuePropertyMappingStrategy.IGNORE` for partial updates) |

**Domain relationships:**
- `User` (1) → `Post` (many): `CascadeType.ALL` + `orphanRemoval` — deleting a user deletes all their posts
- `Post` (1) → `Comment` (many)
- `User` (1) → `Comment` (many)

**Security package** (`com.app.Security`):

Authentication is stateless (no sessions). The flow has two distinct paths:

1. **OAuth2 login** (`/oauth2/**`): Browser hits Google → Spring OAuth2 client → `CustomOidcUserService` upserts the user in the DB → `OAuth2AuthenticationSuccessHandler` generates a JWT → redirects to `http://localhost:3000/oauth2/redirect?token=<jwt>` (hardcoded React dev URL).

2. **API requests**: Bearer JWT in `Authorization` header → `JwtAuthenticationFilter` (runs before `UsernamePasswordAuthenticationFilter`) validates the token via `JwtUtils`, loads the user via `CustomUserDetailsService`, and sets a `UserPrincipal` in the `SecurityContext`.

`UserPrincipal` is the `Authentication` principal in both flows. Controllers use `@AuthenticationPrincipal UserPrincipal principal` to get the current user's ID without an extra DB lookup.

**API routes:**

| Path | Auth | Description |
|---|---|---|
| `GET/POST api/posts` | Required | All posts |
| `GET/PATCH/DELETE api/posts/{id}` | Required | Single post |
| `GET api/v1/users` | Required | All users |
| `GET api/v1/users/{id}` | Required | Public profile |
| `GET api/v1/users/{id}/posts` | Required | User's posts |
| `GET/PATCH/DELETE api/v1/users/me` | Required | Own profile |
| `GET api/v1/users/me/posts` | Required | Own posts |
| `GET/POST/DELETE api/v1/comments` | Required | Comments |
| `/`, `/error`, `/api/v1/auth/**`, `/oauth2/**` | Public | — |

**Entities do not use Lombok class annotations** — getters, setters, and constructors are written manually even though Lombok is on the classpath (it is used only as an annotation processor for MapStruct compatibility).

## Testing

Integration tests (`UserControllerFullIntegrationTest`) use `RestTestClient` with `@SpringBootTest(webEnvironment = RANDOM_PORT)` and hit a real PostgreSQL database. Each test clears the `posts` and `users` tables in `@BeforeEach`. There are no mocks at the repository layer.
