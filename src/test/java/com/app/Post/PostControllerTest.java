package com.app.Post;

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
@DisplayName("PostController Unit Tests")
class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

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
                .standaloneSetup(postController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new ControllerExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getLists — GET /api/v1/posts")
    class GetLists {

        @Test
        @DisplayName("should return 200 with list of posts")
        void shouldReturn200WithListOfPosts() throws Exception {
            PostResponse response = new PostResponse(1L, "Game", "Desc", 50, "http://img.com/img.jpg", 1L, "ACTIVE", LocalDateTime.now());
            when(postService.getPosts()).thenReturn(List.of(response));

            mockMvc.perform(get("/api/v1/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].productName").value("Game"));
        }

        @Test
        @DisplayName("should return 200 with empty list when no posts exist")
        void shouldReturn200WithEmptyList_WhenNoPosts() throws Exception {
            when(postService.getPosts()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("should have @ResponseStatus(OK) annotation")
        void shouldHaveOkResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = PostController.class
                    .getMethod("getLists")
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.OK, annotation.value());
        }
    }

    @Nested
    @DisplayName("getPostById — GET /api/v1/posts/{id}")
    class GetPostById {

        @Test
        @DisplayName("should return 200 with post when valid ID is provided")
        void shouldReturn200WithPost_WhenValidId() throws Exception {
            PostResponse response = new PostResponse(1L, "Game", "Desc", 50, "http://img.com/img.jpg", 1L, "ACTIVE", LocalDateTime.now());
            when(postService.getPostResponseById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.productName").value("Game"));
        }

        @Test
        @DisplayName("should return 404 when post is not found")
        void shouldReturn404_WhenPostNotFound() throws Exception {
            when(postService.getPostResponseById(99L)).thenThrow(new NoSuchElementException("Post not found"));

            mockMvc.perform(get("/api/v1/posts/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 4xx when ID is non-numeric (cannot be parsed)")
        void shouldReturn4xx_WhenIdIsNonNumeric() throws Exception {
            mockMvc.perform(get("/api/v1/posts/null"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("should have @ResponseStatus(OK) annotation")
        void shouldHaveOkResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = PostController.class
                    .getMethod("getPostById", Long.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.OK, annotation.value());
        }
    }

    @Nested
    @DisplayName("addNewPost — POST /api/v1/posts")
    class AddNewPost {

        @Test
        @DisplayName("should return 201 with created post when request is valid")
        void shouldReturn201WithCreatedPost_WhenValid() throws Exception {
            PostRequest request = new PostRequest("Game", 50, "http://img.com/img.jpg", "desc");
            PostResponse response = new PostResponse(1L, "Game", "desc", 50, "http://img.com/img.jpg", 1L, "ACTIVE", LocalDateTime.now());
            when(postService.insertPost(any(), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productName").value("Game"));
        }

        @Test
        @DisplayName("should return 400 when request body is missing")
        void shouldReturn400_WhenBodyIsMissing() throws Exception {
            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when product name is blank")
        void shouldReturn400_WhenProductNameIsBlank() throws Exception {
            PostRequest request = new PostRequest("", 50, "http://img.com/img.jpg", "desc");

            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.productName").exists());
        }

        @Test
        @DisplayName("should return 400 when product name is too short")
        void shouldReturn400_WhenProductNameIsTooShort() throws Exception {
            PostRequest request = new PostRequest("ab", 50, "http://img.com/img.jpg", "desc");

            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.productName").value("Product name must be between 3 and 100 characters"));
        }

        @Test
        @DisplayName("should have @ResponseStatus(CREATED) annotation")
        void shouldHaveCreatedResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = PostController.class
                    .getMethod("addNewPost", UserPrincipal.class, PostRequest.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.CREATED, annotation.value());
        }
    }

    @Nested
    @DisplayName("deletePost — DELETE /api/v1/posts/{id}")
    class DeletePost {

        @Test
        @DisplayName("should return 204 when post is deleted by owner")
        void shouldReturn204_WhenOwnerDeletes() throws Exception {
            mockMvc.perform(delete("/api/v1/posts/1"))
                    .andExpect(status().isNoContent());

            verify(postService).deletePostById(1L, 1L);
        }

        @Test
        @DisplayName("should return 404 when post is not found")
        void shouldReturn404_WhenPostNotFound() throws Exception {
            doThrow(new NoSuchElementException("Post not found")).when(postService).deletePostById(99L, 1L);

            mockMvc.perform(delete("/api/v1/posts/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should have @ResponseStatus(NO_CONTENT) annotation")
        void shouldHaveNoContentResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = PostController.class
                    .getMethod("deletePost", UserPrincipal.class, Long.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.NO_CONTENT, annotation.value());
        }
    }

    @Nested
    @DisplayName("updatePostFromDto — PATCH /api/v1/posts/{id}")
    class UpdatePostFromDto {

        @Test
        @DisplayName("should return 204 when update is valid")
        void shouldReturn204_WhenValidUpdate() throws Exception {
            PostUpdateDto dto = new PostUpdateDto("New Name", "New Desc", 100, "http://img.com/img.jpg", "ACTIVE");

            mockMvc.perform(patch("/api/v1/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNoContent());

            verify(postService).updatePostFromDto(eq(1L), any(), eq(1L));
        }

        @Test
        @DisplayName("should return 400 when request body is missing")
        void shouldReturn400_WhenBodyIsMissing() throws Exception {
            mockMvc.perform(patch("/api/v1/posts/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when product name is blank")
        void shouldReturn400_WhenProductNameIsBlank() throws Exception {
            PostUpdateDto dto = new PostUpdateDto("", "Desc", 100, "http://img.com/img.jpg", "ACTIVE");

            mockMvc.perform(patch("/api/v1/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.productName").exists());
        }

        @Test
        @DisplayName("should return 404 when post is not found")
        void shouldReturn404_WhenPostNotFound() throws Exception {
            PostUpdateDto dto = new PostUpdateDto("Name", "Desc", 100, "http://img.com/img.jpg", "ACTIVE");
            doThrow(new NoSuchElementException("Post not found"))
                    .when(postService).updatePostFromDto(eq(99L), any(), eq(1L));

            mockMvc.perform(patch("/api/v1/posts/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should have @ResponseStatus(NO_CONTENT) annotation")
        void shouldHaveNoContentResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = PostController.class
                    .getMethod("updatePostFromDto", UserPrincipal.class, Long.class, PostUpdateDto.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.NO_CONTENT, annotation.value());
        }
    }
}
