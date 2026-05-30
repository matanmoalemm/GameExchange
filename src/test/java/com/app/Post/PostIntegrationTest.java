package com.app.Post;

import com.app.BaseIntegrationTest;
import com.app.User.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
}
