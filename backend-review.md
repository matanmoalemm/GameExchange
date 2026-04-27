# Senior Backend Engineer Review - Game Exchange Project

## 1. Post.java (Domain Entity)
*   **Critical:**
    *   **Domain-DTO Leaks:** The constructor `public Post(PostRequest postRequest)` couples your core Domain Entity to a Web DTO. This violates Clean Architecture. Mapping should occur in the Service or a dedicated Mapper.
    *   **Circular References:** `equals` and `hashCode` include the `user` object. If `User`'s `hashCode` includes `posts`, you will trigger a `StackOverflowError` during collection operations. Use only the `id` for identity.
*   **Style:**
    *   **Boilerplate:** Use **Lombok** (`@Getter`, `@Setter`, `@NoArgsConstructor`) to reduce the 100+ lines of boilerplate.
    *   **Field Initialization:** `createdAt` is initialized at the field level AND in the constructor. Prefer field-level or `@PrePersist`.

## 2. PostService.java (Business Logic)
*   **Critical:**
    *   **Transaction Boundaries:** `insertPost` and `deletePostById` lack `@Transactional`. While `SimpleJpaRepository` is transactional for `save()`, custom logic (like fetching a User before saving a Post) should be wrapped in a single transaction to ensure atomicity.
    *   **Hardcoded Deletion Logic:** In `markAsSold`, you manually delete from `postRepository` and save to `postArchiveRepository`. This is a "Soft Delete" pattern; consider using a state machine or a single table with a "sold" status if history is critical.
*   **Performance:**
    *   **N+1 Risk:** `getPosts()` returns all fields. As the database grows, this will become slow. Consider **Pagination** (`Pageable`) and **Projections/DTOs** to avoid loading heavy `TEXT` descriptions when listing.
*   **Style:**
    *   **Consistent Exceptions:** You use `NoSuchElementException`. In a larger system, custom business exceptions (e.g., `PostNotFoundException`) are preferred for better `@ControllerAdvice` targeting.

## 3. PostController.java (Web Layer)
*   **Critical:**
    *   **Entity Exposure:** Returning `List<Post>` directly from `getLists()` exposes your database schema to the API. If you add a `password` field to `User` later, it might leak through the `Post -> User` relationship. **Always return DTOs.**
*   **Performance:**
    *   **In-Body vs. In-Param:** `updatePrice` uses `@RequestParam`, while `updateDescription` uses `@RequestBody`. For consistency and REST standards, single fields are often better handled via a PATCH request with a DTO or specific path variables.
*   **Style:**
    *   **Response Codes:** Methods like `addNewPost` return `void` (HTTP 200). Standard REST practice for creation is `HTTP 201 Created` with the location header of the new resource.

## 4. UserService.java / User.java
*   **Critical:**
    *   **Orphan Removal Risks:** `cascade = CascadeType.ALL` on `posts` is dangerous. If a user is deleted, all their trade history (posts) vanishes. Usually, you'd want to archive or "deactivate" the user instead.
*   **Performance:**
    *   **Lazy Loading:** Ensure `posts` list is `FetchType.LAZY` (which is default for `@OneToMany`). Accessing `user.getPosts()` inside `getUserPostsById` will trigger a second query; consider a Join Fetch if you need the data frequently.
*   **Style:**
    *   **Validation Location:** `@Email` validation is present in the Service method parameter. While functional, it's more robust to have this in the `UserRequest` DTO and the Entity itself.

## 5. ControllerExceptionHandler.java (Global Error Handling)
*   **Critical:**
    *   **Incomplete Coverage:** You only handle `NoSuchElementException`. Validation errors (from `@Valid`) will still return default Spring error pages with potentially sensitive stack traces.
*   **Style:**
    *   **Modern API:** You are using `ErrorResponse.create`, which is great (RFC 7807). Expand this to handle `MethodArgumentNotValidException` to provide users with specific field-level validation errors.

---

## Executive Summary Recommendation
1. **Introduce DTOs:** Create `PostResponse` and `UserResponse` classes. Never let Entities leave the Service layer.
2. **Add Lombok:** Clean up the Entity files to focus on the schema rather than getters/setters.
3. **Refine Transactions:** Move `@Transactional` to the class level in Services, or ensure every "write" operation is explicitly marked.
4. **Pagination:** Update your `findAll()` calls to accept `Pageable` to future-proof the application.
