package com.app;

import com.app.Comment.Comment;
import com.app.Comment.CommentRepository;
import com.app.Post.Post;
import com.app.Post.PostRepository;
import com.app.Security.JwtUtils;
import com.app.Security.UserPrincipal;
import com.app.User.User;
import com.app.User.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired protected MockMvc mockMvc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PostRepository postRepository;
    @Autowired protected CommentRepository commentRepository;
    @Autowired protected JwtUtils jwtUtils;

    @BeforeEach
    void cleanDatabase() {
        // deleteAllInBatch issues direct DELETE SQL, avoiding Hibernate cascade confusion.
        // Order respects FK constraints: comments → posts → users.
        commentRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    protected User createUser(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        return userRepository.save(user);
    }

    protected Post createPost(User owner, String productName, int price) {
        Post post = new Post();
        post.setUser(owner);
        post.setProductName(productName);
        post.setPrice(price);
        post.setPicUrl("http://example.com/img.jpg");
        return postRepository.save(post);
    }

    protected Comment createComment(User author, Post post, String text) {
        Comment comment = new Comment();
        comment.setUser(author);
        comment.setPost(post);
        comment.setText(text);
        return commentRepository.save(comment);
    }

    protected String tokenFor(User user) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), List.of());
        return jwtUtils.generateToken(principal);
    }

    protected MockHttpServletRequestBuilder withToken(
            MockHttpServletRequestBuilder request, User user) {
        return request.header("Authorization", "Bearer " + tokenFor(user));
    }
}
