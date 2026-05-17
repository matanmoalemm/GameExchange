package com.example.Post;

import com.example.Comment.CommentRepository;
import com.example.Security.JwtUtils;
import com.example.Security.UserPrincipal;
import com.example.user.User;
import com.example.user.UserRepository;
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
public class PostControllerFullIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private User testUser;
    private User otherUser;
    private String testToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        testUser = userRepository.save(user);
        testToken = generateToken(testUser);

        User other = new User();
        other.setUsername("otheruser");
        other.setEmail("other@example.com");
        otherUser = userRepository.save(other);
        otherToken = generateToken(otherUser);
    }

    // =========================
    // POST /api/v1/posts
    // =========================
    @Test
    void shouldCreatePost() {
        PostRequest request = new PostRequest(
                "Gaming Console",
                500,
                "http://example.com/console.jpg",
                "A powerful gaming console"
        );

        restTestClient.post()
                .uri("/api/v1/posts")
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PostResponse.class)
                .value(response -> {
                    assertThat(response.id()).isNotNull();
                    assertThat(response.productName()).isEqualTo("Gaming Console");
                    assertThat(response.userId()).isEqualTo(testUser.getId());
                });
    }

    @Test
    void shouldNotCreatePostWhenValidationFails() {
        PostRequest request = new PostRequest(
                "ab",            // too short — @Size min=3
                -1,              // negative — @PositiveOrZero
                "not-a-url",     // invalid — @Pattern
                "a".repeat(1001) // too long — @Size max=1000
        );

        restTestClient.post()
                .uri("/api/v1/posts")
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.productName").isEqualTo("Product name must be between 3 and 100 characters")
                .jsonPath("$.price").isEqualTo("Price must be 0 or greater")
                .jsonPath("$.picUrl").isEqualTo("PicUrl must be a valid URL")
                .jsonPath("$.description").isEqualTo("Description cannot exceed 1000 characters");
    }

    @Test
    void shouldReturn401WhenCreatingPostWithoutToken() {
        PostRequest request = new PostRequest("Game", 100, "http://example.com/img.jpg", "desc");

        restTestClient.post()
                .uri("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // =========================
    // GET /api/v1/posts
    // =========================
    @Test
    void shouldGetAllPosts() {
        createPost("Post 1", 100);
        createPost("Post 2", 200);

        restTestClient.get()
                .uri("/api/v1/posts")
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class)
                .value(list -> assertThat(list).hasSize(2));
    }

    @Test
    void shouldGetEmptyListWhenNoPostsExist() {
        restTestClient.get()
                .uri("/api/v1/posts")
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class)
                .value(list -> assertThat(list).hasSize(0));
    }

    // =========================
    // GET /api/v1/posts/{id}
    // =========================
    @Test
    void shouldGetPostById() {
        Post post = createPost("Post 1", 100);

        restTestClient.get()
                .uri("/api/v1/posts/{id}", post.getId())
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PostResponse.class)
                .value(response -> {
                    assertThat(response.productName()).isEqualTo("Post 1");
                    assertThat(response.price()).isEqualTo(100);
                });
    }

    @Test
    void shouldReturn404WhenGetPostByIdNotFound() {
        restTestClient.get()
                .uri("/api/v1/posts/9999")
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    // =========================
    // PATCH /api/v1/posts/{id}
    // =========================
    @Test
    void shouldUpdatePost() {
        Post post = createPost("Old Name", 100);
        PostUpdateDto updateDto = new PostUpdateDto("New Name", "New Description", 150, "http://example.com/new.jpg", "SOLD");

        restTestClient.patch()
                .uri("/api/v1/posts/{id}", post.getId())
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isNoContent();

        Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updatedPost.getProductName()).isEqualTo("New Name");
        assertThat(updatedPost.getPrice()).isEqualTo(150);
        assertThat(updatedPost.getStatus()).isEqualTo("SOLD");
    }

    @Test
    void shouldUpdatePostPartially() {
        Post post = createPost("Initial Name", 100);
        post.setDescription("Initial Description");
        postRepository.save(post);

        PostUpdateDto updateDto = new PostUpdateDto(null, null, 250, null, "PENDING");

        restTestClient.patch()
                .uri("/api/v1/posts/{id}", post.getId())
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isNoContent();

        Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updatedPost.getProductName()).isEqualTo("Initial Name"); // unchanged
        assertThat(updatedPost.getDescription()).isEqualTo("Initial Description"); // unchanged
        assertThat(updatedPost.getPrice()).isEqualTo(250);
        assertThat(updatedPost.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldNotUpdatePostWhenValidationFails() {
        Post post = createPost("Valid Post", 100);
        PostUpdateDto updateDto = new PostUpdateDto(
                "ab",
                "a".repeat(1001),
                -5,
                "ftp://invalid",
                "INVALID_STATUS"
        );

        restTestClient.patch()
                .uri("/api/v1/posts/{id}", post.getId())
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.productName").isEqualTo("Product name must be between 3 and 100 characters")
                .jsonPath("$.description").isEqualTo("Description cannot exceed 1000 characters")
                .jsonPath("$.price").isEqualTo("Price must be 0 or greater")
                .jsonPath("$.status").isEqualTo("Invalid status. Must be ACTIVE, PENDING, SOLD, or DELETED");
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentPost() {
        PostUpdateDto updateDto = new PostUpdateDto("Name", "Desc", 100, "http://img.com/img.jpg", "ACTIVE");

        restTestClient.patch()
                .uri("/api/v1/posts/9999")
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn403WhenPatchingPostNotOwned() {
        Post post = createPost("Some Post", 100);
        PostUpdateDto updateDto = new PostUpdateDto("Hacked", null, null, null, null);

        restTestClient.patch()
                .uri("/api/v1/posts/{id}", post.getId())
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isForbidden();
    }

    // =========================
    // DELETE /api/v1/posts/{id}
    // =========================
    @Test
    void shouldDeletePost() {
        Post post = createPost("Some Post", 100);

        restTestClient.delete()
                .uri("/api/v1/posts/{id}", post.getId())
                .header("Authorization", "Bearer " + testToken)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(postRepository.existsById(post.getId())).isFalse();
    }

    @Test
    void shouldReturn403WhenDeletingPostNotOwned() {
        Post post = createPost("Some Post", 100);

        restTestClient.delete()
                .uri("/api/v1/posts/{id}", post.getId())
                .header("Authorization", "Bearer " + otherToken)
                .exchange()
                .expectStatus().isForbidden();

        assertThat(postRepository.existsById(post.getId())).isTrue();
    }

    // =========================
    // Helpers
    // =========================
    private Post createPost(String name, Integer price) {
        Post post = new Post();
        post.setProductName(name);
        post.setPrice(price);
        post.setPicUrl("http://example.com/img.jpg");
        post.setUser(testUser);
        post.setStatus("ACTIVE");
        return postRepository.save(post);
    }

    private String generateToken(User user) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), List.of());
        return jwtUtils.generateToken(principal);
    }
}
