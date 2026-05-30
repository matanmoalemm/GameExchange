package com.app.User;

import com.app.ControllerExceptionHandler;
import com.app.Post.PostResponse;
import com.app.Security.UserPrincipal;
import com.app.user.UserController;
import com.app.user.UserResponse;
import com.app.user.UserService;
import com.app.user.UserUpdateDTO;
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
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

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
                .standaloneSetup(userController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new ControllerExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getMyProfile — GET /api/v1/users/me")
    class GetMyProfile {

        @Test
        @DisplayName("should return 200 with authenticated user's profile")
        void shouldReturn200WithUserResponse() throws Exception {
            UserResponse response = new UserResponse(1L, "testuser");
            when(userService.getUserResponseById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("testuser"));
        }

        @Test
        @DisplayName("should return 404 when user is not found")
        void shouldReturn404_WhenUserNotFound() throws Exception {
            when(userService.getUserResponseById(1L)).thenThrow(new NoSuchElementException("User not found"));

            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should have @ResponseStatus(OK) annotation")
        void shouldHaveOkResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = UserController.class
                    .getMethod("getMyProfile", UserPrincipal.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.OK, annotation.value());
        }

    }

    @Nested
    @DisplayName("getMyPosts — GET /api/v1/users/me/posts")
    class GetMyPosts {

        @Test
        @DisplayName("should return 200 with list of authenticated user's posts")
        void shouldReturn200WithPostList() throws Exception {
            PostResponse post = new PostResponse(1L, "Game", "Desc", 50, "http://img.com/img.jpg", 1L, "ACTIVE", LocalDateTime.now());
            when(userService.getUserPostsById(1L)).thenReturn(List.of(post));

            mockMvc.perform(get("/api/v1/users/me/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].productName").value("Game"));
        }

        @Test
        @DisplayName("should return 200 with empty list when user has no posts")
        void shouldReturn200WithEmptyList_WhenNoPosts() throws Exception {
            when(userService.getUserPostsById(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/users/me/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("should have @ResponseStatus(OK) annotation")
        void shouldHaveOkResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = UserController.class
                    .getMethod("getMyPosts", UserPrincipal.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.OK, annotation.value());
        }
    }

    @Nested
    @DisplayName("updateMyProfile — PATCH /api/v1/users/me")
    class UpdateMyProfile {

        @Test
        @DisplayName("should return 204 when update is valid")
        void shouldReturn204_WhenValidDto() throws Exception {
            UserUpdateDTO dto = new UserUpdateDTO("newname", "new@test.com");

            mockMvc.perform(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNoContent());

            verify(userService).updateUserFromDto(eq(1L), any());
        }

        @Test
        @DisplayName("should return 400 when request body is missing")
        void shouldReturn400_WhenBodyIsMissing() throws Exception {
            mockMvc.perform(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when name is too short (blank value)")
        void shouldReturn400_WhenNameIsBlank() throws Exception {
            UserUpdateDTO dto = new UserUpdateDTO("  ", "valid@test.com");

            mockMvc.perform(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should have @ResponseStatus(NO_CONTENT) annotation")
        void shouldHaveNoContentResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = UserController.class
                    .getMethod("updateMyProfile", UserPrincipal.class, UserUpdateDTO.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.NO_CONTENT, annotation.value());
        }
    }

    @Nested
    @DisplayName("deleteMyAccount — DELETE /api/v1/users/me")
    class DeleteMyAccount {

        @Test
        @DisplayName("should return 204 when account is deleted")
        void shouldReturn204_WhenValid() throws Exception {
            mockMvc.perform(delete("/api/v1/users/me"))
                    .andExpect(status().isNoContent());

            verify(userService).deleteUserById(1L);
        }

        @Test
        @DisplayName("should have @ResponseStatus(NO_CONTENT) annotation")
        void shouldHaveNoContentResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = UserController.class
                    .getMethod("deleteMyAccount", UserPrincipal.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.NO_CONTENT, annotation.value());
        }
    }

    @Nested
    @DisplayName("getAllUsers — GET /api/v1/users")
    class GetAllUsers {

        @Test
        @DisplayName("should return 200 with list of all users")
        void shouldReturn200WithUserList() throws Exception {
            UserResponse user = new UserResponse(1L, "john");
            when(userService.getUsers()).thenReturn(List.of(user));

            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("john"));
        }

        @Test
        @DisplayName("should return 200 with empty list when no users exist")
        void shouldReturn200WithEmptyList_WhenNoUsers() throws Exception {
            when(userService.getUsers()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("should have @ResponseStatus(OK) annotation")
        void shouldHaveOkResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = UserController.class
                    .getMethod("getAllUsers")
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.OK, annotation.value());
        }
    }

    @Nested
    @DisplayName("getUserPublicProfile — GET /api/v1/users/{id}")
    class GetUserPublicProfile {

        @Test
        @DisplayName("should return 200 with user profile when valid ID is provided")
        void shouldReturn200WithUserResponse_WhenValidId() throws Exception {
            UserResponse response = new UserResponse(2L, "otheruser");
            when(userService.getUserResponseById(2L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/users/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.name").value("otheruser"));
        }

        @Test
        @DisplayName("should return 404 when user is not found")
        void shouldReturn404_WhenUserNotFound() throws Exception {
            when(userService.getUserResponseById(99L)).thenThrow(new NoSuchElementException("User not found"));

            mockMvc.perform(get("/api/v1/users/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 4xx when ID is non-numeric")
        void shouldReturn4xx_WhenIdIsNonNumeric() throws Exception {
            mockMvc.perform(get("/api/v1/users/null"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("should have @ResponseStatus(OK) annotation")
        void shouldHaveOkResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = UserController.class
                    .getMethod("getUserPublicProfile", Long.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.OK, annotation.value());
        }
    }

    @Nested
    @DisplayName("getPostsByUserId — GET /api/v1/users/{id}/posts")
    class GetPostsByUserId {

        @Test
        @DisplayName("should return 200 with user's posts when valid ID is provided")
        void shouldReturn200WithPostList_WhenValidId() throws Exception {
            PostResponse post = new PostResponse(1L, "Game", "Desc", 50, "http://img.com/img.jpg", 2L, "ACTIVE", LocalDateTime.now());
            when(userService.getUserPostsById(2L)).thenReturn(List.of(post));

            mockMvc.perform(get("/api/v1/users/2/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("should return 404 when user is not found")
        void shouldReturn404_WhenUserNotFound() throws Exception {
            when(userService.getUserPostsById(99L)).thenThrow(new NoSuchElementException("User not found"));

            mockMvc.perform(get("/api/v1/users/99/posts"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should have @ResponseStatus(OK) annotation")
        void shouldHaveOkResponseStatusAnnotation() throws Exception {
            ResponseStatus annotation = UserController.class
                    .getMethod("getPostsByUserId", Long.class)
                    .getAnnotation(ResponseStatus.class);
            assertNotNull(annotation);
            assertEquals(HttpStatus.OK, annotation.value());
        }
    }
}
