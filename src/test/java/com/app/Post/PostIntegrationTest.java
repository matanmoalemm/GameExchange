package com.app.Post;

import com.app.BaseIntegrationTest;
import com.app.User.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Post Integration Tests")
class PostIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("JWT Security Pipeline")
    class JwtSecurityPipeline {

        @Test
        @DisplayName("POST /api/v1/posts without token returns 401 — JwtAuthenticationFilter is wired")
        void shouldReturn401_WhenNoToken() throws Exception {
            PostRequest request = new PostRequest("Test Game", 50, "http://example.com/img.jpg", "desc");

            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/v1/posts with tampered JWT signature returns 401")
        void shouldReturn401_WhenTokenTampered() throws Exception {
            User user = createUser("owner@test.com", "owner");
            String validToken = tokenFor(user);
            String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";
            PostRequest request = new PostRequest("Test Game", 50, "http://example.com/img.jpg", "desc");

            mockMvc.perform(post("/api/v1/posts")
                            .header("Authorization", "Bearer " + tamperedToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/v1/posts without token returns 200 — permitAll() is wired")
        void shouldReturn200_WhenGetPostsWithNoToken() throws Exception {
            mockMvc.perform(get("/api/v1/posts"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Search Posts")
    class SearchPosts {

        @Test
        @DisplayName("GET /api/v1/posts/search?name=witcher — returns only matching posts")
        void shouldReturnMatchingPosts() throws Exception {
            User user = createUser("owner@test.com", "owner");
            PostRequest request1 = new PostRequest("The Witcher 3", 50, "http://example.com/witcher.jpg", "rpg");
            PostRequest request2 = new PostRequest("FIFA 23", 60, "http://example.com/fifa.jpg", "sports");

            // Insert posts
            mockMvc.perform(withToken(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)), user))
                    .andExpect(status().isCreated());

            mockMvc.perform(withToken(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)), user))
                    .andExpect(status().isCreated());

            // Search
            mockMvc.perform(get("/api/v1/posts/search")
                            .param("name", "witcher"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].productName").value("The Witcher 3"));
        }

        @Test
        @DisplayName("GET /api/v1/posts/search?name=nonexistent — returns empty list")
        void shouldReturnEmptyListWhenNoMatches() throws Exception {
            mockMvc.perform(get("/api/v1/posts/search")
                            .param("name", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Database State")
    class DatabaseState {

        @Test
        @DisplayName("POST /api/v1/posts — row persisted with correct user association")
        void shouldPersistPost_WithCorrectOwner() throws Exception {
            User user = createUser("owner@test.com", "owner");
            PostRequest request = new PostRequest("Test Game", 50, "http://example.com/img.jpg", "desc");

            mockMvc.perform(withToken(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)), user))
                    .andExpect(status().isCreated());

            assertEquals(1, postRepository.count());
            Post saved = postRepository.findAll().get(0);
            assertEquals(user.getId(), saved.getUser().getId());
        }

        @Test
        @DisplayName("POST /api/v1/posts — status defaults to ACTIVE in DB")
        void shouldDefaultStatusToActive() throws Exception {
            User user = createUser("owner@test.com", "owner");
            PostRequest request = new PostRequest("Test Game", 50, "http://example.com/img.jpg", "desc");

            mockMvc.perform(withToken(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)), user))
                    .andExpect(status().isCreated());

            Post saved = postRepository.findAll().get(0);
            assertEquals("ACTIVE", saved.getStatus());
        }

        @Test
        @DisplayName("DELETE /api/v1/posts/{id} — row removed from DB")
        void shouldDeletePost_FromDB() throws Exception {
            User user = createUser("owner@test.com", "owner");
            Post post = createPost(user, "Test Game", 50);

            mockMvc.perform(withToken(delete("/api/v1/posts/" + post.getId()), user))
                    .andExpect(status().isNoContent());

            assertEquals(0, postRepository.count());
        }
    }

    @Nested
    @DisplayName("Auditing")
    class Auditing {

        @Test
        @DisplayName("Post.createdAt is populated by @CreationTimestamp on persist")
        void shouldSetCreatedAt_OnCreation() throws Exception {
            User user = createUser("owner@test.com", "owner");
            PostRequest request = new PostRequest("Test Game", 50, "http://example.com/img.jpg", "desc");

            mockMvc.perform(withToken(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)), user))
                    .andExpect(status().isCreated());

            Post saved = postRepository.findAll().get(0);
            assertNotNull(saved.getCreatedAt());
            assertThat(saved.getCreatedAt())
                    .isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("MapStruct Partial Update (NullValuePropertyMappingStrategy.IGNORE)")
    class MapStructPartialUpdate {

        @Test
        @DisplayName("PATCH with only price — all other fields are unchanged in DB")
        void shouldKeepOtherFields_WhenOnlyPricePatched() throws Exception {
            User owner = createUser("owner@test.com", "owner");
            Post post = createPost(owner, "Original Title", 50);
            PostUpdateDto dto = new PostUpdateDto(null, null, 30, null, null, null);

            mockMvc.perform(withToken(patch("/api/v1/posts/" + post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)), owner))
                    .andExpect(status().isNoContent());

            Post updated = postRepository.findById(post.getId()).orElseThrow();
            assertEquals(30, updated.getPrice());
            assertEquals("Original Title", updated.getProductName());
            assertEquals("http://example.com/img.jpg", updated.getPicUrl());
            assertEquals("ACTIVE", updated.getStatus());
        }

        @Test
        @DisplayName("PATCH with all-null body — post is unchanged (no-op)")
        void shouldBeNoOp_WhenAllFieldsNull() throws Exception {
            User owner = createUser("owner@test.com", "owner");
            Post post = createPost(owner, "Original Title", 50);
            PostUpdateDto dto = new PostUpdateDto(null, null, null, null, null, null);

            mockMvc.perform(withToken(patch("/api/v1/posts/" + post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)), owner))
                    .andExpect(status().isNoContent());

            Post unchanged = postRepository.findById(post.getId()).orElseThrow();
            assertEquals("Original Title", unchanged.getProductName());
            assertEquals(50, unchanged.getPrice());
            assertEquals("http://example.com/img.jpg", unchanged.getPicUrl());
            assertEquals("ACTIVE", unchanged.getStatus());
        }
    }

    @Nested
    @DisplayName("Cascade: Post → Comment")
    class CascadePostToComment {

        @Test
        @DisplayName("Deleting a Post cascade-deletes all its Comments")
        void shouldCascadeDeleteComments_WhenPostDeleted() throws Exception {
            User user = createUser("owner@test.com", "owner");
            Post post = createPost(user, "Test Game", 50);
            createComment(user, post, "Comment 1");
            createComment(user, post, "Comment 2");

            assertEquals(2, commentRepository.count());

            mockMvc.perform(withToken(delete("/api/v1/posts/" + post.getId()), user))
                    .andExpect(status().isNoContent());

            assertEquals(0, postRepository.count());
            assertEquals(0, commentRepository.count());
        }
    }

    @Nested
    @DisplayName("High-Throughput Scenarios")
    class HighThroughputScenarios {

        @Test
        @DisplayName("1,000 concurrent mixed-entity requests produce no 5xx errors and correct status codes")
        void shouldHandleHighConcurrentLoad_WithoutServerErrors() throws Exception {
            // 10 regular users — persist throughout the test; each mutates only their own resources
            int regularCount = 10;
            User[] regularUsers = new User[regularCount];
            String[] regularTokens = new String[regularCount];
            for (int i = 0; i < regularCount; i++) {
                regularUsers[i] = createUser("regular" + i + "@test.com", "regular" + i);
                regularTokens[i] = tokenFor(regularUsers[i]);
            }

            // 10 power users — each pre-seeded with 3 posts + 3 comments; deleted once during the test
            int powerCount = 10;
            String[] powerTokens = new String[powerCount];
            for (int i = 0; i < powerCount; i++) {
                User powerUser = createUser("power" + i + "@test.com", "power" + i);
                powerTokens[i] = tokenFor(powerUser);
                Post pwPost1 = createPost(powerUser, "Power Game " + i + "A", 100);
                Post pwPost2 = createPost(powerUser, "Power Game " + i + "B", 200);
                Post pwPost3 = createPost(powerUser, "Power Game " + i + "C", 300);
                createComment(powerUser, pwPost1, "Power comment " + i + "1");
                createComment(powerUser, pwPost2, "Power comment " + i + "2");
                createComment(powerUser, pwPost3, "Power comment " + i + "3");
            }

            // Shared post owned by regular_0 — target for all reads and concurrent comment writes
            Post popularPost = createPost(regularUsers[0], "Popular Game", 100);
            final long popularPostId = popularPost.getId();

            String newPostBody = objectMapper.writeValueAsString(
                    new PostRequest("Concurrent Game", 50, "http://example.com/img.jpg", "desc"));
            String patchBody = "{\"name\":null,\"email\":null}";

            ExecutorService pool = Executors.newFixedThreadPool(20);
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            // Bucket 1 — 200 GET /api/v1/posts (public, no auth)
            for (int i = 0; i < 200; i++) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(get("/api/v1/posts"))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            // Bucket 2 — 200 GET /api/v1/posts/{id} (public, no auth)
            for (int i = 0; i < 200; i++) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(get("/api/v1/posts/" + popularPostId))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            // Bucket 3 — 200 GET /api/v1/comments/post/{id} (authenticated)
            for (int i = 0; i < 200; i++) {
                final String token = regularTokens[i % regularCount];
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(get("/api/v1/comments/post/" + popularPostId)
                                        .header("Authorization", "Bearer " + token))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            // Bucket 4 — 125 POST /api/v1/posts — each user creates their own post
            for (int i = 0; i < 125; i++) {
                final String token = regularTokens[i % regularCount];
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(post("/api/v1/posts")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(newPostBody))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            // Bucket 5 — 125 POST /api/v1/comments — each user comments on the popular post
            for (int i = 0; i < 125; i++) {
                final String token = regularTokens[i % regularCount];
                final String commentBody = "{\"postId\":" + popularPostId + ",\"text\":\"concurrent comment\"}";
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(post("/api/v1/comments")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(commentBody))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            // Bucket 6 — 70 GET /api/v1/users/me — each user reads their own profile
            for (int i = 0; i < 70; i++) {
                final String token = regularTokens[i % regularCount];
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(get("/api/v1/users/me")
                                        .header("Authorization", "Bearer " + token))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            // Bucket 7 — 70 PATCH /api/v1/users/me — all-null no-op; each user patches only their own profile
            for (int i = 0; i < 70; i++) {
                final String token = regularTokens[i % regularCount];
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(patch("/api/v1/users/me")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(patchBody))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            // Bucket 8 — 10 DELETE /api/v1/users/me — each power user deleted exactly once,
            // cascade-deleting their 3 posts + 3 comments concurrently with the reads above
            for (int i = 0; i < powerCount; i++) {
                final String token = powerTokens[i];
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(delete("/api/v1/users/me")
                                        .header("Authorization", "Bearer " + token))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            pool.shutdown();

            List<Integer> statuses = futures.stream().map(f -> f.getNow(-1)).toList();

            assertThat(statuses).doesNotContain(500);
            assertThat(statuses.subList(0, 600)).allMatch(s -> s == 200);    // all reads
            assertThat(statuses.subList(600, 850)).allMatch(s -> s == 201);  // all creates
            assertThat(statuses.subList(850, 920)).allMatch(s -> s == 200);  // GET /users/me
            assertThat(statuses.subList(920, 990)).allMatch(s -> s == 204);  // PATCH /users/me
            assertThat(statuses.subList(990, 1000)).allMatch(s -> s == 204); // DELETE /users/me
            assertTrue(postRepository.existsById(popularPost.getId()));
        }
    }
}
