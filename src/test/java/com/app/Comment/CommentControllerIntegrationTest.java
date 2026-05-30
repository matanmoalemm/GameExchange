package com.app.Comment;

import com.app.Post.Post;
import com.app.Post.PostRepository;
import com.app.Security.JwtUtils;
import com.app.Security.UserPrincipal;
import com.app.User.User;
import com.app.User.UserRepository;
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
public class CommentControllerIntegrationTest {

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

    private User postOwner;
    private User commenter;
    private Post post;
    private String postOwnerToken;
    private String commenterToken;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        User owner = new User();
        owner.setUsername("postowner");
        owner.setEmail("owner@example.com");
        postOwner = userRepository.save(owner);
        postOwnerToken = generateToken(postOwner);

        User comm = new User();
        comm.setUsername("commenter");
        comm.setEmail("commenter@example.com");
        commenter = userRepository.save(comm);
        commenterToken = generateToken(commenter);

        Post p = new Post();
        p.setProductName("Gaming Console");
        p.setPrice(500);
        p.setPicUrl("http://example.com/img.jpg");
        p.setUser(postOwner);
        p.setStatus("ACTIVE");
        post = postRepository.save(p);
    }

    // =========================
    // POST /api/v1/comments
    // =========================
    @Test
    void shouldAddComment() {
        CommentRequest request = new CommentRequest(post.getId(), "Great game!");

        restTestClient.post()
                .uri("/api/v1/comments")
                .header("Authorization", "Bearer " + commenterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated();

        List<Comment> saved = commentRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getText()).isEqualTo("Great game!");
        assertThat(saved.get(0).getUser().getId()).isEqualTo(commenter.getId());
        assertThat(saved.get(0).getPost().getId()).isEqualTo(post.getId());
    }

    @Test
    void shouldReturn400WhenCommentTextIsBlank() {
        CommentRequest request = new CommentRequest(post.getId(), "");

        restTestClient.post()
                .uri("/api/v1/comments")
                .header("Authorization", "Bearer " + commenterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.text").isEqualTo("Comment text is required");
    }

    @Test
    void shouldReturn400WhenPostIdIsMissing() {
        CommentRequest request = new CommentRequest(null, "Some text");

        restTestClient.post()
                .uri("/api/v1/comments")
                .header("Authorization", "Bearer " + commenterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.postId").isEqualTo("Post ID is required");
    }

    @Test
    void shouldReturn401WhenAddingCommentWithoutToken() {
        CommentRequest request = new CommentRequest(post.getId(), "Nice!");

        restTestClient.post()
                .uri("/api/v1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // =========================
    // GET /api/v1/comments/post/{postId}
    // =========================
    @Test
    void shouldGetComments_WhenPostOwner() {
        addComment("Great game!");

        restTestClient.get()
                .uri("/api/v1/comments/post/{postId}", post.getId())
                .header("Authorization", "Bearer " + postOwnerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class)
                .value(list -> assertThat(list).hasSize(1));
    }

    @Test
    void shouldReturn403_WhenNonOwnerGetsComments() {
        addComment("Great game!");

        restTestClient.get()
                .uri("/api/v1/comments/post/{postId}", post.getId())
                .header("Authorization", "Bearer " + commenterToken) // not the post owner
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturnEmptyList_WhenPostHasNoComments() {
        restTestClient.get()
                .uri("/api/v1/comments/post/{postId}", post.getId())
                .header("Authorization", "Bearer " + postOwnerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class)
                .value(list -> assertThat(list).isEmpty());
    }

    // =========================
    // DELETE /api/v1/comments/{id}
    // =========================
    @Test
    void shouldDeleteComment_WhenAuthor() {
        addComment("I'll delete this");

        Comment saved = commentRepository.findAll().get(0);

        restTestClient.delete()
                .uri("/api/v1/comments/{id}", saved.getId())
                .header("Authorization", "Bearer " + commenterToken)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(commentRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    void shouldReturn403_WhenNonAuthorDeletesComment() {
        addComment("Leave this alone");

        Comment saved = commentRepository.findAll().get(0);

        restTestClient.delete()
                .uri("/api/v1/comments/{id}", saved.getId())
                .header("Authorization", "Bearer " + postOwnerToken) // not the comment author
                .exchange()
                .expectStatus().isForbidden();

        assertThat(commentRepository.existsById(saved.getId())).isTrue(); // still exists
    }

    // =========================
    // Helpers
    // =========================
    private void addComment(String text) {
        Comment comment = new Comment();
        comment.setText(text);
        comment.setPost(post);
        comment.setUser(commenter);
        commentRepository.save(comment);
    }

    private String generateToken(User user) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), List.of());
        return jwtUtils.generateToken(principal);
    }
}
