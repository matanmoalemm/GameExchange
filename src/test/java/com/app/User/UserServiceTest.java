package com.app.User;

import com.app.Post.Post;
import com.app.Post.PostMapper;
import com.app.Post.PostResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private UserLookupService userLookupService;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("getUsers")
    class GetUsers {

        @Test
        @DisplayName("should return all users as mapped responses")
        void shouldReturnAllUsersAsMappedResponses() {
            User user = new User();
            UserResponse response = new UserResponse(1L, "john");

            when(userRepository.findAll()).thenReturn(List.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            List<UserResponse> result = userService.getUsers();

            assertEquals(1, result.size());
            assertEquals("john", result.get(0).name());
            verify(userRepository).findAll();
            verify(userMapper).toResponse(user);
        }

        @Test
        @DisplayName("should return empty list when no users exist")
        void shouldReturnEmptyList_WhenNoUsers() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserResponse> result = userService.getUsers();

            assertTrue(result.isEmpty());
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("should handle large number of users without degradation")
        void shouldHandleLargeNumberOfUsers() {
            List<User> users = Collections.nCopies(1000, new User());
            UserResponse response = new UserResponse(1L, "user");

            when(userRepository.findAll()).thenReturn(users);
            when(userMapper.toResponse(any())).thenReturn(response);

            List<UserResponse> result = userService.getUsers();

            assertEquals(1000, result.size());
            verify(userMapper, times(1000)).toResponse(any());
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return user when valid ID is provided")
        void shouldReturnUser_WhenValidId() {
            User user = new User();
            user.setId(1L);

            when(userLookupService.getById(1L)).thenReturn(user);

            User result = userService.getUserById(1L);

            assertEquals(1L, result.getId());
            verify(userLookupService).getById(1L);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when user is not found")
        void shouldThrow_WhenUserNotFound() {
            when(userLookupService.getById(1L)).thenThrow(new NoSuchElementException("User not found"));

            assertThrows(NoSuchElementException.class, () -> userService.getUserById(1L));
            verify(userLookupService).getById(1L);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when ID is null")
        void shouldThrow_WhenIdIsNull() {
            when(userLookupService.getById(null)).thenThrow(new NoSuchElementException("User not found"));

            assertThrows(NoSuchElementException.class, () -> userService.getUserById(null));
        }
    }

    @Nested
    @DisplayName("getUserResponseById")
    class GetUserResponseById {

        @Test
        @DisplayName("should return UserResponse when valid ID is provided")
        void shouldReturnUserResponse_WhenValidId() {
            User user = new User();
            UserResponse response = mock(UserResponse.class);

            when(userLookupService.getById(1L)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(response);

            UserResponse result = userService.getUserResponseById(1L);

            assertSame(response, result);
            verify(userMapper).toResponse(user);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when user is not found")
        void shouldThrow_WhenUserNotFound() {
            when(userLookupService.getById(1L)).thenThrow(new NoSuchElementException("User not found"));

            assertThrows(NoSuchElementException.class, () -> userService.getUserResponseById(1L));
        }

        @Test
        @DisplayName("should throw NoSuchElementException when ID is null")
        void shouldThrow_WhenIdIsNull() {
            when(userLookupService.getById(null)).thenThrow(new NoSuchElementException("User not found"));

            assertThrows(NoSuchElementException.class, () -> userService.getUserResponseById(null));
        }
    }

    @Nested
    @DisplayName("deleteUserById")
    class DeleteUserById {

        @Test
        @DisplayName("should delete user when valid ID is provided")
        void shouldDeleteUser_WhenValidId() {
            userService.deleteUserById(1L);

            verify(userRepository).deleteById(1L);
            verifyNoMoreInteractions(userRepository);
        }


    }

    @Nested
    @DisplayName("insertUser")
    class InsertUser {

        @Test
        @DisplayName("should insert user when request is valid")
        void shouldInsertUser_WhenValidRequest() {
            UserRequest request = new UserRequest("john", "john@test.com");
            User user = new User();

            when(userMapper.toEntity(request)).thenReturn(user);

            userService.insertUser(request);

            verify(userMapper).toEntity(request);
            verify(userRepository).save(user);
        }
    }

    @Nested
    @DisplayName("getUserPostsById")
    class GetUserPostsById {

        @Test
        @DisplayName("should return user's posts when user has posts")
        void shouldReturnUserPosts_WhenUserHasPosts() {
            User user = new User();
            Post post = new Post();
            user.setPosts(List.of(post));
            PostResponse postResponse = new PostResponse(1L, "Item", "Desc", 10, "url", 1L, "ACTIVE", LocalDateTime.now());

            when(userLookupService.getById(1L)).thenReturn(user);
            when(userMapper.toPostResponses(user.getPosts())).thenReturn(List.of(postResponse));

            List<PostResponse> result = userService.getUserPostsById(1L);

            assertEquals(1, result.size());
            assertEquals("Item", result.get(0).productName());
            verify(userLookupService).getById(1L);
        }

        @Test
        @DisplayName("should return empty list when user has no posts")
        void shouldReturnEmptyList_WhenUserHasNoPosts() {
            User user = new User();
            user.setPosts(List.of());

            when(userLookupService.getById(1L)).thenReturn(user);
            when(userMapper.toPostResponses(List.of())).thenReturn(List.of());

            List<PostResponse> result = userService.getUserPostsById(1L);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should throw NoSuchElementException when user is not found")
        void shouldThrow_WhenUserNotFound() {
            when(userLookupService.getById(1L)).thenThrow(new NoSuchElementException("User not found"));

            assertThrows(NoSuchElementException.class, () -> userService.getUserPostsById(1L));
        }


        @Test
        @DisplayName("should handle large number of posts without degradation")
        void shouldHandleLargeNumberOfPosts() {
            User user = new User();
            List<Post> posts = Collections.nCopies(1000, new Post());
            user.setPosts(posts);
            List<PostResponse> responses = Collections.nCopies(1000, mock(PostResponse.class));

            when(userLookupService.getById(1L)).thenReturn(user);
            when(userMapper.toPostResponses(posts)).thenReturn(responses);

            List<PostResponse> result = userService.getUserPostsById(1L);

            assertEquals(1000, result.size());
        }
    }

    @Nested
    @DisplayName("updateUserFromDto")
    class UpdateUserFromDto {

        @Test
        @DisplayName("should update user when valid ID and DTO are provided")
        void shouldUpdateUser_WhenValidIdAndDto() {
            User user = new User();
            UserUpdateDTO dto = new UserUpdateDTO("new_user", "new@mail.com");

            when(userLookupService.getById(1L)).thenReturn(user);

            userService.updateUserFromDto(1L, dto);

            verify(userLookupService).getById(1L);
            verify(userMapper).updateUserFromDto(dto, user);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when user is not found")
        void shouldThrow_WhenUserNotFound() {
            when(userLookupService.getById(1L)).thenThrow(new NoSuchElementException("User not found"));
            UserUpdateDTO dto = new UserUpdateDTO("new_user", "new@mail.com");

            assertThrows(NoSuchElementException.class, () -> userService.updateUserFromDto(1L, dto));
            verify(userMapper, never()).updateUserFromDto(any(), any());
        }


    }
}
