package com.example.Post;

import com.example.user.User;
import com.example.user.UserRepository;
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
public class PostControllerFullIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        testUser = userRepository.save(user);
    }

    @Test
    void shouldCreatePostIsSuccessfulWhenUserExists() {
        PostRequest request = new PostRequest(
                "Gaming Console",
                500,
                "http://example.com/console.jpg",
                testUser.getId(),
                "A powerful gaming console"
        );

        restTestClient.post()
                .uri("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated();

        List<Post> posts = postRepository.findAll();
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).getProductName()).isEqualTo("Gaming Console");
        assertThat(posts.get(0).getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void shouldNotCreatePostWhenUserDoesNotExist() {
        PostRequest request = new PostRequest(
                "Gaming Console",
                500,
                "http://example.com/console.jpg",
                9999, // Non-existent user
                "A powerful gaming console"
        );

        restTestClient.post()
                .uri("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldNotCreatePostWhenValidationFailsCompletely() {
        // Testing all constraints: Blanks, invalid sizes, negative numbers, invalid URLs, and nulls
        PostRequest request = new PostRequest(
                " ", // NotBlank
                -1,  // PositiveOrZero
                "not-a-url", // Pattern
                null, // NotNull
                "a".repeat(1001) // Size max 1000
        );

        restTestClient.post()
                .uri("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.productName").isEqualTo("Product name must be between 3 and 100 characters")
                .jsonPath("$.price").isEqualTo("Price must be 0 or greater")
                .jsonPath("$.picUrl").isEqualTo("PicUrl must be a valid URL")
                .jsonPath("$.userId").isEqualTo("User ID is required")
                .jsonPath("$.description").isEqualTo("Description cannot exceed 1000 characters");
    }

    @Test
    void shouldGetAllPosts() {
        createPost("Post 1", 100);
        createPost("Post 2", 200);

        restTestClient.get()
                .uri("/api/v1/posts")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FOUND)
                .expectBody(List.class)
                .value(list -> assertThat(list).hasSize(2));
    }

    @Test
    void shouldGetEmptyListWhenNoPostsExist() {
        restTestClient.get()
                .uri("/api/v1/posts")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FOUND)
                .expectBody(List.class)
                .value(list -> assertThat(list).hasSize(0));
    }

    @Test
    void shouldGetPostById() {
        Post post = createPost("Post 1", 100);

        restTestClient.get()
                .uri("/api/v1/posts/{id}", post.getId())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FOUND)
                .expectBody(PostResponse.class)
                .value(response -> {
                    assertThat(response.productName()).isEqualTo("Post 1");
                    assertThat(response.price()).isEqualTo(100);
                });
    }

    @Test
    void shouldReturn400WhenGetPostByIdNotFound() {
        restTestClient.get()
                .uri("/api/v1/posts/9999")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldUpdatePost() {
        Post post = createPost("Old Name", 100);
        PostUpdateDto updateDto = new PostUpdateDto(
                "New Name",
                "New Description",
                150,
                "http://example.com/new.jpg",
                "SOLD"
        );

        restTestClient.patch()
                .uri("/api/v1/posts/{id}", post.getId())
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
    void shouldNotUpdatePostWhenValidationFails() {
        Post post = createPost("Valid Post", 100);
        PostUpdateDto updateDto = new PostUpdateDto(
                "ab", // Too short
                "a".repeat(1001), // Too long
                -5, // Negative
                "ftp://invalid", 
                "INVALID_STATUS"
        );

        restTestClient.patch()
                .uri("/api/v1/posts/{id}", post.getId())
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
    void shouldUpdatePostPartially() {
        Post post = createPost("Initial Name", 100);
        post.setDescription("Initial Description");
        postRepository.save(post);

        // Only update price and status
        PostUpdateDto updateDto = new PostUpdateDto(
                null, // No name update
                null, // No description update
                250,
                null, // No picUrl update
                "PENDING"
        );

        restTestClient.patch()
                .uri("/api/v1/posts/{id}", post.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isNoContent();

        Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updatedPost.getProductName()).isEqualTo("Initial Name"); // Should remain unchanged
        assertThat(updatedPost.getDescription()).isEqualTo("Initial Description"); // Should remain unchanged
        assertThat(updatedPost.getPrice()).isEqualTo(250);
        assertThat(updatedPost.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldReturn400WhenUpdatingNonExistentPost() {
        PostUpdateDto updateDto = new PostUpdateDto("Name", "Desc", 100, "http://img.com", "ACTIVE");

        restTestClient.patch()
                .uri("/api/v1/posts/9999")
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    private Post createPost(String name, Integer price) {
        Post post = new Post();
        post.setProductName(name);
        post.setPrice(price);
        post.setPicUrl("http://example.com/img.jpg");
        post.setUser(testUser);
        post.setStatus("ACTIVE");
        return postRepository.save(post);
    }
}
