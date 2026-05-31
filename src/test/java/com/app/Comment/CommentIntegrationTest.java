package com.app.Comment;

import com.app.BaseIntegrationTest;
import com.app.Post.Post;
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

@DisplayName("Comment Integration Tests")
class CommentIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("JWT Security Pipeline")
    class JwtSecurityPipeline {

        @Test
        @DisplayName("POST /api/v1/comments without token returns 401")
        void shouldReturn401_WhenNoToken() throws Exception {
            CommentRequest request = new CommentRequest(1L, "Hello");

            mockMvc.perform(post("/api/v1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/v1/comments/post/{id} without token returns 401")
        void shouldReturn401_WhenGetCommentsWithNoToken() throws Exception {
            mockMvc.perform(get("/api/v1/comments/post/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Database State")
    class DatabaseState {

        @Test
        @DisplayName("POST /api/v1/comments — row persisted with correct user and post associations")
        void shouldPersistComment_WithCorrectAssociations() throws Exception {
            User user = createUser("commenter@test.com", "commenter");
            User postOwner = createUser("owner@test.com", "owner");
            Post targetPost = createPost(postOwner, "Test Game", 50);
            CommentRequest request = new CommentRequest(targetPost.getId(), "Great game!");

            mockMvc.perform(withToken(post("/api/v1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)), user))
                    .andExpect(status().isCreated());

            assertEquals(1, commentRepository.count());
            Comment saved = commentRepository.findAll().get(0);
            assertEquals("Great game!", saved.getText());
            assertEquals(user.getId(), saved.getUser().getId());
            assertEquals(targetPost.getId(), saved.getPost().getId());
        }

        @Test
        @DisplayName("DELETE /api/v1/comments/{id} — row removed from DB")
        void shouldDeleteComment_FromDB() throws Exception {
            User user = createUser("commenter@test.com", "commenter");
            Post post = createPost(user, "Test Game", 50);
            Comment comment = createComment(user, post, "My comment");

            mockMvc.perform(withToken(delete("/api/v1/comments/" + comment.getId()), user))
                    .andExpect(status().isNoContent());

            assertEquals(0, commentRepository.count());
        }
    }

    @Nested
    @DisplayName("Auditing")
    class Auditing {

        @Test
        @DisplayName("Comment.createdAt is populated by @CreationTimestamp on persist")
        void shouldSetCreatedAt_OnCreation() throws Exception {
            User user = createUser("commenter@test.com", "commenter");
            Post targetPost = createPost(user, "Test Game", 50);
            CommentRequest request = new CommentRequest(targetPost.getId(), "Great game!");

            mockMvc.perform(withToken(post("/api/v1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)), user))
                    .andExpect(status().isCreated());

            Comment saved = commentRepository.findAll().get(0);
            assertNotNull(saved.getCreatedAt());
            assertThat(saved.getCreatedAt())
                    .isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("Repository Isolation: findByPostId")
    class RepositoryIsolation {

        @Test
        @DisplayName("GET /api/v1/comments/post/{id} returns only comments for that post")
        void shouldReturnOnlyCommentsForRequestedPost() throws Exception {
            User user = createUser("user@test.com", "user");
            Post post1 = createPost(user, "Game One", 50);
            Post post2 = createPost(user, "Game Two", 30);
            createComment(user, post1, "Comment A on post 1");
            createComment(user, post1, "Comment B on post 1");
            createComment(user, post2, "Comment on post 2");

            mockMvc.perform(withToken(get("/api/v1/comments/post/" + post1.getId()), user))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    @Nested
    @DisplayName("Transactional Boundaries")
    class TransactionalBoundaries {

        @Test
        @DisplayName("insertComment with non-existent postId returns 404 and leaves no comment in DB")
        void shouldNotPersistComment_WhenPostNotFound() throws Exception {
            User user = createUser("user@test.com", "user");
            CommentRequest request = new CommentRequest(99999L, "Hello");

            mockMvc.perform(withToken(post("/api/v1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)), user))
                    .andExpect(status().isNotFound());

            assertEquals(0, commentRepository.count());
        }

        @Test
        @DisplayName("deleteCommentById rejected for non-owner leaves comment in DB")
        void shouldNotDeleteComment_WhenRequesterIsNotOwner() throws Exception {
            User author = createUser("author@test.com", "author");
            User intruder = createUser("intruder@test.com", "intruder");
            Post post = createPost(author, "Test Game", 50);
            Comment comment = createComment(author, post, "Author's comment");

            mockMvc.perform(withToken(delete("/api/v1/comments/" + comment.getId()), intruder))
                    .andExpect(status().isForbidden());

            assertEquals(1, commentRepository.count());
        }

        @Test
        @DisplayName("updatePostFromDto rejected for non-owner leaves post data unchanged in DB")
        void shouldNotUpdatePost_WhenRequesterIsNotOwner() throws Exception {
            User owner = createUser("owner@test.com", "owner");
            User intruder = createUser("intruder@test.com", "intruder");
            Post post = createPost(owner, "Original Title", 50);
            com.app.Post.PostUpdateDto dto = new com.app.Post.PostUpdateDto("New Title", null, null, null, null);

            mockMvc.perform(withToken(patch("/api/v1/posts/" + post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)), intruder))
                    .andExpect(status().isForbidden());

            Post unchanged = postRepository.findById(post.getId()).orElseThrow();
            assertEquals("Original Title", unchanged.getProductName());
        }
    }

    @Nested
    @DisplayName("High-Throughput Scenarios")
    class HighThroughputScenarios {

        @Test
        @DisplayName("325 concurrent comment reads and writes produce no 5xx errors")
        void shouldHandleConcurrentCommentLoad_WithoutServerErrors() throws Exception {
            int userCount = 10;
            User[] users = new User[userCount];
            String[] tokens = new String[userCount];
            User postOwner = createUser("owner@test.com", "owner");
            Post sharedPost = createPost(postOwner, "Shared Game", 50);
            final long postId = sharedPost.getId();

            for (int i = 0; i < userCount; i++) {
                users[i] = createUser("commenter" + i + "@test.com", "commenter" + i);
                tokens[i] = tokenFor(users[i]);
            }

            ExecutorService pool = Executors.newFixedThreadPool(userCount);
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            // 200 concurrent reads of the shared post's comment list
            for (int i = 0; i < 200; i++) {
                final String token = tokens[i % userCount];
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(get("/api/v1/comments/post/" + postId)
                                        .header("Authorization", "Bearer " + token))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            // 125 concurrent comment creates — each user writes their own comment on the shared post
            for (int i = 0; i < 125; i++) {
                final String token = tokens[i % userCount];
                final String body = "{\"postId\":" + postId + ",\"text\":\"concurrent comment " + i + "\"}";
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return mockMvc.perform(post("/api/v1/comments")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) { return 500; }
                }, pool));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            pool.shutdown();

            List<Integer> statuses = futures.stream().map(f -> f.getNow(-1)).toList();
            assertThat(statuses).doesNotContain(500);
            assertThat(statuses.subList(0, 200)).allMatch(s -> s == 200);
            assertThat(statuses.subList(200, 325)).allMatch(s -> s == 201);
            assertThat(commentRepository.count()).isGreaterThanOrEqualTo(125);
        }
    }
}
