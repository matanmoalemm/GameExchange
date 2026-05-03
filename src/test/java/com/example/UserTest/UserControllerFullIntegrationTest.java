package com.example.UserTest;

import com.example.Post.Post;
import com.example.Post.PostRepository;
import com.example.Post.PostResponse;
import com.example.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUser() {
        UserRequest request = new UserRequest("john_doe", "john@example.com");

        restTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated();

        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("john_doe");
    }

    @Test
    void shouldNotCreateUserWhenValidationFailsCompletely() {
        // Blank username/email, too short username, invalid regex, invalid email format
        UserRequest request = new UserRequest("ai", "not-an-email");

        restTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.username").isEqualTo("Username must be between 3 and 20 characters")
                .jsonPath("$.email").isEqualTo("Please provide a valid email address");
    }

    @Test
    void shouldNotCreateUserWhenFieldsAreBlank() {
        UserRequest request = new UserRequest("", "");

        restTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.username").isEqualTo("Username must be between 3 and 20 characters")
                .jsonPath("$.email").isEqualTo("Email is required");
    }

    @Test
    void shouldNotCreateUserWhenUsernamePatternFails() {
        UserRequest request = new UserRequest("user space", "valid@example.com");

        restTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.username").isEqualTo("Username can only contain letters, numbers, dots, and underscores");
    }

    @Test
    void shouldNotUpdateUserWhenValidationFails() {
        User user = createUser("valid_user", "valid@example.com");
        UserUpdateDTO updateDto = new UserUpdateDTO("ab", "invalid-email");

        restTestClient.patch()
                .uri("/api/v1/users/{id}", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.username").isEqualTo("Username must be between 3 and 20 characters")
                .jsonPath("$.email").isEqualTo("Please provide a valid email address");
    }

    @Test
    void shouldGetAllUsers() {
        createUser("user1", "user1@example.com");
        createUser("user2", "user2@example.com");

        restTestClient.get()
                .uri("/api/v1/users")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FOUND)
                .expectBody(List.class)
                .value(list -> assertThat(list).hasSize(2));
    }

    @Test
    void shouldGetUserById() {
        User user = createUser("john_doe", "john@example.com");

        restTestClient.get()
                .uri("/api/v1/users/{id}", user.getId())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FOUND)
                .expectBody(UserResponse.class)
                .value(response -> {
                    assertThat(response.username()).isEqualTo("john_doe");
                });
    }

    @Test
    void shouldReturn400WhenUserNotFound() {
        restTestClient.get()
                .uri("/api/v1/users/9999")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldGetUserPosts() {
        User user = createUser("john_doe", "john@example.com");
        createPost("Post 1", 100, user);
        createPost("Post 2", 200, user);

        restTestClient.get()
                .uri("/api/v1/users/{id}/posts", user.getId())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FOUND)
                .expectBody(List.class)
                .value(list -> assertThat(list).hasSize(2));
    }

    @Test
    void shouldDeleteUserAndItsPosts() {
        User user = createUser("john_doe", "john@example.com");
        createPost("Post 1", 100, user);
        createPost("Post 2", 200, user);

        assertThat(postRepository.findAll()).hasSize(2);

        restTestClient.delete()
                .uri("/api/v1/users/{id}", user.getId())
                .exchange()
                .expectStatus().isNoContent();

        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(postRepository.findAll()).isEmpty();
    }

    @Test
    void shouldUpdateUser() {
        User user = createUser("old_username", "old@example.com");
        UserUpdateDTO updateDto = new UserUpdateDTO("new_username", "new@example.com");

        restTestClient.patch()
                .uri("/api/v1/users/{id}", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isNoContent();

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getUsername()).isEqualTo("new_username");
        assertThat(updatedUser.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void shouldUpdateUserPartially() {
        User user = createUser("initial_user", "initial@example.com");

        // Only update email
        UserUpdateDTO updateDto = new UserUpdateDTO(null, "new@example.com");

        restTestClient.patch()
                .uri("/api/v1/users/{id}", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isNoContent();

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getUsername()).isEqualTo("initial_user"); // Should remain unchanged
        assertThat(updatedUser.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void shouldReturn400WhenUpdatingNonExistentUser() {
        UserUpdateDTO updateDto = new UserUpdateDTO("new_user", "new@example.com");

        restTestClient.patch()
                .uri("/api/v1/users/9999")
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

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
}
