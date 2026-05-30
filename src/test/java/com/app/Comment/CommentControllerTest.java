package com.app.Comment;

import com.app.ControllerExceptionHandler;
import com.app.Security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentController Unit Tests")
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        principal = new UserPrincipal(1L, "test@test.com", List.of());
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc = MockMvcBuilders
                .standaloneSetup(commentController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new ControllerExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCommentsByPost — GET /api/v1/comments/post/{postId}")
    class GetCommentsByPost {

        @Test
        @DisplayName("should return 200 with list of comments when requester is post owner")
        void shouldReturn200WithComments_WhenOwner() throws Exception {
            CommentResponse response = new CommentResponse(1L, "Nice game!", 1L, 1L, LocalDateTime.now());
            when(commentService.getCommentsByPostId(1L, 1L)).thenReturn(List.of(response));

            mockMvc.perform(get("/api/v1/comments/post/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].text").value("Nice game!"));
        }

        @Test
        @DisplayName("should return 200 with empty list when post has no comments")
        void shouldReturn200WithEmptyList_WhenNoComments() throws Exception {
            when(commentService.getCommentsByPostId(1L, 1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/comments/post/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("should return 404 when post is not found")
        void shouldReturn404_WhenPostNotFound() throws Exception {
            when(commentService.getCommentsByPostId(99L, 1L)).thenThrow(new NoSuchElementException("Post not found"));

            mockMvc.perform(get("/api/v1/comments/post/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 4xx when post ID is non-numeric")
        void shouldReturn4xx_WhenPostIdIsNonNumeric() throws Exception {
            mockMvc.perform(get("/api/v1/comments/post/null"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("should have @ResponseStatus(OK) annotation")
        void shouldHaveOkResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = CommentController.class
                    .getMethod("getCommentsByPost", UserPrincipal.class, Long.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.OK, annotation.value());
        }
    }

    @Nested
    @DisplayName("addNewComment — POST /api/v1/comments")
    class AddNewComment {

        @Test
        @DisplayName("should return 201 with created comment when request is valid")
        void shouldReturn201WithCreatedComment_WhenValid() throws Exception {
            CommentRequest request = new CommentRequest(1L, "Great game!");
            CommentResponse response = new CommentResponse(1L, "Great game!", 1L, 1L, LocalDateTime.now());
            when(commentService.insertComment(any(), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.text").value("Great game!"));
        }

        @Test
        @DisplayName("should return 400 when request body is missing")
        void shouldReturn400_WhenBodyIsMissing() throws Exception {
            mockMvc.perform(post("/api/v1/comments")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when comment text is blank")
        void shouldReturn400_WhenTextIsBlank() throws Exception {
            CommentRequest request = new CommentRequest(1L, "");

            mockMvc.perform(post("/api/v1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.text").value("Comment text is required"));
        }

        @Test
        @DisplayName("should return 400 when post ID is null")
        void shouldReturn400_WhenPostIdIsNull() throws Exception {
            CommentRequest request = new CommentRequest(null, "Some text");

            mockMvc.perform(post("/api/v1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.postId").value("Post ID is required"));
        }

        @Test
        @DisplayName("should have @ResponseStatus(CREATED) annotation")
        void shouldHaveCreatedResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = CommentController.class
                    .getMethod("addNewComment", UserPrincipal.class, CommentRequest.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.CREATED, annotation.value());
        }
    }

    @Nested
    @DisplayName("deleteComment — DELETE /api/v1/comments/{id}")
    class DeleteComment {

        @Test
        @DisplayName("should return 204 when comment is deleted by author")
        void shouldReturn204_WhenAuthorDeletes() throws Exception {
            mockMvc.perform(delete("/api/v1/comments/1"))
                    .andExpect(status().isNoContent());

            verify(commentService).deleteCommentById(1L, 1L);
        }

        @Test
        @DisplayName("should return 404 when comment is not found")
        void shouldReturn404_WhenCommentNotFound() throws Exception {
            doThrow(new NoSuchElementException("Comment not found")).when(commentService).deleteCommentById(99L, 1L);

            mockMvc.perform(delete("/api/v1/comments/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 4xx when comment ID is non-numeric")
        void shouldReturn4xx_WhenIdIsNonNumeric() throws Exception {
            mockMvc.perform(delete("/api/v1/comments/null"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("should have @ResponseStatus(NO_CONTENT) annotation")
        void shouldHaveNoContentResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = CommentController.class
                    .getMethod("deleteComment", UserPrincipal.class, Long.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.NO_CONTENT, annotation.value());
        }
    }
}
