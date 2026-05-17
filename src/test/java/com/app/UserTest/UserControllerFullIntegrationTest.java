package com.app.UserTest;

import com.app.Comment.CommentRepository;
import com.app.Post.Post;
import com.app.Post.PostRepository;
import com.app.Security.JwtUtils;
import com.app.Security.UserPrincipal;
import com.app.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class UserControllerFullIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private User testUser;
    private String testToken;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        testUser = createUser("test_user", "test@example.com");
        testToken = generateToken(testUser);
    }

    // =========================
    // GET /me
    // =========================
    @Test
    void shouldGetMyProfile() {
        restTestClient.get()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .value(response -> assertThat(response.name()).isEqualTo("test_user"));
    }

    @Test
    void shouldReturn401WhenNoToken() {
        restTestClient.get()
                .uri("/api/v1/users/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // =========================
    // GET /me/posts
    // =========================
    @Test
    void shouldGetMyPosts() {
        createPost("Post 1", 100, testUser);
        createPost("Post 2", 200, testUser);

        restTestClient.get()
                .uri("/api/v1/users/me/posts")
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class)
                .value(list -> assertThat(list).hasSize(2));
    }

    // =========================
    // GET / (all users)
    // =========================
    @Test
    void shouldGetAllUsers() {
        createUser("user2", "user2@example.com");

        restTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class)
                .value(list -> assertThat(list).hasSize(2));
    }

    // =========================
    // GET /{id}
    // =========================
    @Test
    void shouldGetUserPublicProfile() {
        restTestClient.get()
                .uri("/api/v1/users/{id}", testUser.getId())
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .value(response -> assertThat(response.name()).isEqualTo("test_user"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() {
        restTestClient.get()
                .uri("/api/v1/users/9999")
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    // =========================
    // GET /{id}/posts
    // =========================
    @Test
    void shouldGetPostsByUserId() {
        createPost("Post 1", 100, testUser);
        createPost("Post 2", 200, testUser);

        restTestClient.get()
                .uri("/api/v1/users/{id}/posts", testUser.getId())
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class)
                .value(list -> assertThat(list).hasSize(2));
    }

    // =========================
    // PATCH /me
    // =========================
    @Test
    void shouldUpdateMyProfile() {
        UserUpdateDTO updateDto = new UserUpdateDTO("new_username", "new@example.com");

        restTestClient.patch()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isNoContent();

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getUsername()).isEqualTo("new_username");
        assertThat(updatedUser.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void shouldUpdateMyProfilePartially() {
        UserUpdateDTO updateDto = new UserUpdateDTO(null, "new@example.com");

        restTestClient.patch()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isNoContent();

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getUsername()).isEqualTo("test_user"); // unchanged
        assertThat(updatedUser.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void shouldNotUpdateWhenValidationFails() {
        UserUpdateDTO updateDto = new UserUpdateDTO("ab", "invalid-email");

        restTestClient.patch()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Username must be between 3 and 20 characters")
                .jsonPath("$.email").isEqualTo("Please provide a valid email address");
    }

    // =========================
    // DELETE /me
    // =========================
    @Test
    void shouldDeleteMyAccount() {
        createPost("Post 1", 100, testUser);
        createPost("Post 2", 200, testUser);

        assertThat(postRepository.findAll()).hasSize(2);

        restTestClient.delete()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(userRepository.existsById(testUser.getId())).isFalse();
        assertThat(postRepository.findAll()).isEmpty();
    }

    // =========================
    // Helpers
    // =========================
    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        return userRepository.save(user);
    }

    private void createPost(String name, Integer price, User user) {
        Post post = new Post();
        post.setProductName(name);
        post.setPrice(price);
        post.setPicUrl("http://example.com/img.jpg");
        post.setUser(user);
        post.setStatus("ACTIVE");
        postRepository.save(post);
    }

    private String generateToken(User user) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), List.of());
        return jwtUtils.generateToken(principal);
    }
}
