# Game Exchange

A RESTful backend for a game marketplace platform where users can list, browse, and comment on video games they want to sell or trade. Built with Spring Boot and secured with Google OAuth2 + JWT.

---

## Technologies

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security, Google OAuth2 (OIDC), JWT (JJWT 0.12.3) |
| Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Bean Validation |
| Serialization | Jackson |
| Build | Maven |

---

## What is Game Exchange?

Game Exchange is a community-driven marketplace for gamers. Users sign in with their Google account and can:

- **List** games they want to sell or trade, with a title, description, price, and image URL
- **Browse** listings posted by other users
- **Comment** on posts to ask questions or negotiate deals
- **Manage** their own profile and listings

The backend exposes a REST API consumed by a React frontend running on `localhost:3000`.

---

## Security & Authentication

Authentication is **stateless** — the server holds no sessions. 
All the information needed to authenticate you is inside the Jwt Token itself

### Login flow (Google OAuth2)

1. The user navigates to `/oauth2/authorization/google` (handled by Spring's OAuth2 client).
2. Google authenticates the user and returns an OIDC token.
3. `CustomOidcUserService` extracts the user's email and name, then upserts their record in the database.
4. `OAuth2AuthenticationSuccessHandler` generates a signed JWT and redirects the browser to:
   ```
   http://localhost:3000/oauth2/redirect?token=<jwt>
   ```
5. The React client stores the token and attaches it to every subsequent request.

### Authenticated API requests

Every protected endpoint requires an `Authorization: Bearer <token>` header.

The `JwtAuthenticationFilter` runs before Spring's default auth filter on every request:
1. Extracts the token from the `Authorization` header.
2. Validates the signature and expiry via `JwtUtils`.
3. Loads the matching `User` from the database.
4. Injects a `UserPrincipal` into the `SecurityContext`.

Controllers read the current user's ID directly from `@AuthenticationPrincipal UserPrincipal` — no extra database lookup needed.

### Owner-only enforcement

Mutating endpoints (update / delete) compare the authenticated user's ID against the resource's owner ID inside the service layer. A mismatch throws a `403 Forbidden`.

---

## API Reference

### Legend
| Symbol | Meaning |
|---|---|
| Auth | A valid Bearer JWT is required |
| Owner | Must be the authenticated user who owns the resource |
| Public | No token needed |

---

### Posts — `api/v1/posts`

| Method | Path | Auth | Owner | Description |
|---|---|------|---|---|
| `GET` | `/api/v1/posts` | Public    | — | List all posts |
| `GET` | `/api/v1/posts/{id}` | Auth | — | Get a single post |
| `POST` | `/api/v1/posts` | Auth | — | Create a new post (assigned to the authenticated user) |
| `PATCH` | `/api/v1/posts/{id}` | Auth | Owner | Update your post (partial update) |
| `DELETE` | `/api/v1/posts/{id}` | Auth | Owner | Delete your post |

---

### Users — `api/v1/users`

| Method | Path | Auth | Owner | Description |
|---|---|---|---|---|
| `GET` | `/api/v1/users` | Auth | — | List all users |
| `GET` | `/api/v1/users/{id}` | Auth | — | View a user's public profile |
| `GET` | `/api/v1/users/{id}/posts` | Auth | — | View all posts by a specific user |
| `GET` | `/api/v1/users/me` | Auth | Owner | View your own profile |
| `GET` | `/api/v1/users/me/posts` | Auth | Owner | View your own posts |
| `PATCH` | `/api/v1/users/me` | Auth | Owner | Update your profile (partial update) |
| `DELETE` | `/api/v1/users/me` | Auth | Owner | Delete your account (cascades to all your posts) |

---

### Comments — `api/v1/comments`

| Method | Path | Auth | Owner | Description |
|---|---|---|---|---|
| `GET` | `/api/v1/comments/post/{postId}` | Auth | — | Get all comments for a post |
| `POST` | `/api/v1/comments` | Auth | — | Add a comment to a post |
| `DELETE` | `/api/v1/comments/{id}` | Auth | Owner | Delete your comment |

---

### Public routes

| Path | Description |
|---|---|
| `/oauth2/**` | Google OAuth2 login flow |
| `/api/v1/auth/**` | Auth utilities |
| `/`, `/error` | Root and error pages |

---

## DTOs (Data Transfer Objects)

DTOs decouple the internal database entities from what the API actually receives and returns. The entity layer stays private; clients only ever see the DTO shape.

All DTOs are **Java records** — immutable, concise, and automatically serialized by Jackson.

There are three DTO roles used per domain:

| Role | Suffix | Purpose |
|---|---|---|
| **Request** | `*Request` | Carries validated input from the client to create a resource |
| **Update** | `*UpdateDTO` | Carries a partial update — all fields are optional, `null` means "leave unchanged" |
| **Response** | `*Response` | The safe, read-only shape returned to the client — never exposes internal fields like passwords |

### User DTOs

**`UserRequest`** — used when creating a user manually (non-OAuth path)
| Field | Validation |
|---|---|
| `name` | 3–20 chars, letters/numbers/dots/underscores only |
| `email` | Required, must be a valid email format |

**`UserUpdateDTO`** — used on `PATCH /api/v1/users/me`
| Field | Validation |
|---|---|
| `name` | 3–20 chars, letters/numbers/dots/underscores only (optional) |
| `email` | Valid email format (optional) |

**`UserResponse`** — returned on all user endpoints
| Field | Description |
|---|---|
| `id` | User's database ID |
| `name` | Display name |

### Post DTOs

**`PostRequest`** — used on `POST /api/v1/posts`
| Field | Validation |
|---|---|
| `productName` | 3–100 characters |
| `price` | Required, 0 or greater |
| `picUrl` | Required, must be a valid `http/https/ftp` URL |
| `description` | Max 1000 characters |

**`PostResponse`** — returned on all post endpoints
| Field | Description |
|---|---|
| `id` | Post ID |
| `productName` | Game title |
| `description` | Listing description |
| `price` | Listed price |
| `picUrl` | Image URL |
| `userId` | ID of the user who created the post |
| `status` | Listing status (`ACTIVE` by default) |
| `createdAt` | Timestamp of creation |

### Comment DTOs

**`CommentRequest`** — used on `POST /api/v1/comments`
| Field | Validation |
|---|---|
| `postId` | Required, must reference an existing post |
| `text` | Required, max 1000 characters |

**`CommentResponse`** — returned on all comment endpoints
| Field | Description |
|---|---|
| `id` | Comment ID |
| `text` | Comment body |
| `postId` | Post this comment belongs to |
| `authorId` | User who wrote the comment |
| `createdAt` | Timestamp of creation |

---

## Testing

The project uses a **two-tier testing strategy** that balances speed with confidence.

| Tier | Scope | Tools |
|---|---|---|
| Unit tests | Service layer only | JUnit 5, Mockito |
| Integration tests | Full HTTP stack (Controller → Service → DB) | JUnit 5, RestTestClient, real PostgreSQL |

### Technologies

- **JUnit 5** — test framework (`@Test`, `@Nested`, `@BeforeEach`)
- **Mockito** — mocking in unit tests (`@Mock`, `@InjectMocks`, `when/thenReturn`, `verify`)
- **Spring Boot Test** — `@SpringBootTest(webEnvironment = RANDOM_PORT)` boots the full server on a random port
- **RestTestClient** — HTTP client used in integration tests to call live endpoints
- **AssertJ** — fluent assertion library (`assertThat(...)`)

### Integration tests

- Require a **running PostgreSQL** instance — no in-memory substitute.
- `@BeforeEach` deletes all rows in reverse FK order (comments → posts → users) before every test to guarantee isolation.
- A JWT token is generated per test user and sent via `Authorization: Bearer <token>`.
- Scenarios covered: CRUD operations, `401` unauthenticated, `403` ownership violations, `400` validation failures.

### Unit tests

- `@ExtendWith(MockitoExtension.class)` — no Spring context is loaded, tests run fast.
- Repositories are `@Mock`; the service under test gets them via `@InjectMocks`.
- Test cases are grouped with `@Nested` into: `HappyPath`, `Boundary`, `ErrorCases`, and `InteractionTests`.

### Test classes

| Class | Type | Covers |
|---|---|---|
| `UserIntegrationTest` | Integration | User REST API |
| `PostIntegrationTest` | Integration | Post REST API |
| `CommentIntegrationTest` | Integration | Comment REST API |
| `UserServiceTest` | Unit | UserService business logic |
| `PostServiceTest` | Unit | PostService business logic |
| `CommentServiceTest` | Unit | CommentService business logic |
| `GameExchangeApplicationTests` | Smoke | Spring context loads cleanly |
