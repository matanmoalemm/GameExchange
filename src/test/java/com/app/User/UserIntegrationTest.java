package com.app.User;

import com.app.BaseIntegrationTest;
import com.app.Post.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("User Integration Tests")
class UserIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("JWT Security Pipeline")
    class JwtSecurityPipeline {

        @Test
        @DisplayName("GET /api/v1/users/me without token returns 401")
        void shouldReturn401_WhenNoToken() throws Exception {
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/v1/users/me with valid JWT returns 200 — full auth pipeline wired")
        void shouldReturn200_WhenValidToken() throws Exception {
            User user = createUser("me@test.com", "myuser");

            mockMvc.perform(withToken(get("/api/v1/users/me"), user))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId()));
        }
    }

    @Nested
    @DisplayName("Database State")
    class DatabaseState {

        @Test
        @DisplayName("DELETE /api/v1/users/me — user row removed when no posts or comments")
        void shouldDeleteUser_WithNoDependencies() throws Exception {
            User user = createUser("me@test.com", "myuser");

            mockMvc.perform(withToken(delete("/api/v1/users/me"), user))
                    .andExpect(status().isNoContent());

            assertEquals(0, userRepository.count());
        }

        @Test
        @DisplayName("DELETE /api/v1/users/me — cascade deletes owned posts when no comments exist")
        void shouldCascadeDeletePosts_WhenUserDeleted() throws Exception {
            User user = createUser("me@test.com", "myuser");
            createPost(user, "Game 1", 50);
            createPost(user, "Game 2", 30);

            assertEquals(2, postRepository.count());

            mockMvc.perform(withToken(delete("/api/v1/users/me"), user))
                    .andExpect(status().isNoContent());

            assertEquals(0, userRepository.count());
            assertEquals(0, postRepository.count());
        }

        @Test
        @DisplayName("DELETE /api/v1/users/me — full cascade chain: User → Posts → Comments")
        void shouldCascadeDeletePostsAndComments_WhenUserDeleted() throws Exception {
            User user = createUser("me@test.com", "myuser");
            Post post = createPost(user, "Test Game", 50);
            createComment(user, post, "My comment");

            assertEquals(1, commentRepository.count());

            mockMvc.perform(withToken(delete("/api/v1/users/me"), user))
                    .andExpect(status().isNoContent());

            assertEquals(0, commentRepository.count());
            assertEquals(0, postRepository.count());
            assertEquals(0, userRepository.count());
        }

        @Test
        @DisplayName("DELETE /api/v1/users/me — cascade-deletes comments authored by user on other posts")
        void shouldDeleteUserComments_OnOtherPosts_WhenUserDeleted() throws Exception {
            User postOwner = createUser("owner@test.com", "postowner");
            Post post = createPost(postOwner, "Someone's Game", 50);
            User commenter = createUser("commenter@test.com", "commenter");
            createComment(commenter, post, "A comment on someone else's post");

            assertEquals(1, commentRepository.count());

            mockMvc.perform(withToken(delete("/api/v1/users/me"), commenter))
                    .andExpect(status().isNoContent());

            assertEquals(0, commentRepository.count());
            assertEquals(1, userRepository.count()); // postOwner still exists
            assertEquals(1, postRepository.count()); // their post still exists
        }
    }

    @Nested
    @DisplayName("MapStruct Partial Update (NullValuePropertyMappingStrategy.IGNORE)")
    class MapStructPartialUpdate {

        @Test
        @DisplayName("PATCH with only name — email is unchanged in DB")
        void shouldKeepEmail_WhenOnlyNamePatched() throws Exception {
            User user = createUser("original@test.com", "originalname");
            UserUpdateDTO dto = new UserUpdateDTO("newname", null);

            mockMvc.perform(withToken(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)), user))
                    .andExpect(status().isNoContent());

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertEquals("newname", updated.getUsername());
            assertEquals("original@test.com", updated.getEmail());
        }

        @Test
        @DisplayName("PATCH with only email — username is unchanged in DB")
        void shouldKeepUsername_WhenOnlyEmailPatched() throws Exception {
            User user = createUser("original@test.com", "originalname");
            UserUpdateDTO dto = new UserUpdateDTO(null, "updated@test.com");

            mockMvc.perform(withToken(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)), user))
                    .andExpect(status().isNoContent());

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertEquals("originalname", updated.getUsername());
            assertEquals("updated@test.com", updated.getEmail());
        }

        @Test
        @DisplayName("PATCH with all-null body — entity unchanged (no-op)")
        void shouldBeNoOp_WhenAllFieldsNull() throws Exception {
            User user = createUser("original@test.com", "originalname");
            UserUpdateDTO dto = new UserUpdateDTO(null, null);

            mockMvc.perform(withToken(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)), user))
                    .andExpect(status().isNoContent());

            User unchanged = userRepository.findById(user.getId()).orElseThrow();
            assertEquals("originalname", unchanged.getUsername());
            assertEquals("original@test.com", unchanged.getEmail());
        }
    }

    @Nested
    @DisplayName("Database Constraints")
    class DatabaseConstraints {

        @Test
        @DisplayName("Unique email constraint is enforced at DB level")
        void shouldThrow_WhenDuplicateEmailSaved() {
            createUser("duplicate@test.com", "user1");

            User user2 = new User();
            user2.setEmail("duplicate@test.com");
            user2.setUsername("user2");

            assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(user2));
        }
    }

    @Nested
    @DisplayName("High-Throughput Scenarios")
    class HighThroughputScenarios {

        @Test
        @DisplayName("140 concurrent profile-endpoint requests produce no 5xx errors")
        void shouldHandleConcurrentProfileRequests_WithoutServerErrors() throws Exception {
            int userCount = 10;
            User[] users = new User[userCount];
            String[] tokens = new String[userCount];
            for (int i = 0; i < userCount; i++) {
                users[i] = createUser("concurrent" + i + "@test.com", "concurrent" + i);
                tokens[i] = tokenFor(users[i]);
            }

            ExecutorService pool = Executors.newFixedThreadPool(userCount);
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            // 70 concurrent GET /api/v1/users/me — each user reads their own profile
            for (int i = 0; i < 70; i++) {
                final String token = tokens[i % userCount];
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(get("/api/v1/users/me")
                                        .header("Authorization", "Bearer " + token))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            // 70 concurrent PATCH /api/v1/users/me — all-null no-op; each user patches only their own profile
            for (int i = 0; i < 70; i++) {
                final String token = tokens[i % userCount];
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(patch("/api/v1/users/me")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":null,\"email\":null}"))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            pool.shutdown();

            List<Integer> statuses = futures.stream().map(f -> f.getNow(-1)).toList();
            assertThat(statuses).doesNotContain(500);
            assertThat(statuses.subList(0, 70)).allMatch(s -> s == 200);
            assertThat(statuses.subList(70, 140)).allMatch(s -> s == 204);
        }
    }
}
