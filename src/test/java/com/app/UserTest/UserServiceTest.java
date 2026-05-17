package com.app.UserTest;

import com.app.Post.Post;
import com.app.Post.PostMapper;
import com.app.Post.PostResponse;
import com.app.user.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private UserService userService;

    // =========================
    // GET USER BY ID
    // =========================
    @Test
    void shouldReturnUser_WhenUserExists() {
        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertEquals(1L, result.getId());
        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> userService.getUserById(1L));

        verify(userRepository).findById(1L);
    }

    // =========================
    // GET USERS
    // =========================
    @Test
    void shouldReturnAllUsers_AsResponses() {
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
    void shouldReturnEmptyList_WhenNoUsersExist() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> result = userService.getUsers();

        assertTrue(result.isEmpty());
        verify(userRepository).findAll();
    }

    // =========================
    // GET USER RESPONSE BY ID
    // =========================
    @Test
    void shouldReturnUserResponseById() {
        User user = new User();
        UserResponse response = mock(UserResponse.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getUserResponseById(1L);

        assertNotNull(result);
        verify(userMapper).toResponse(user);
    }

    // =========================
    // POSTS BY USER
    // =========================
    @Test
    void shouldReturnUserPostsById() {
        User user = new User();
        Post post = new Post();
        user.setPosts(List.of(post));

        PostResponse postResponse = new PostResponse(
                1L, "Item", "Desc", 10, "url", 1L, "ACTIVE", LocalDateTime.now()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toPostResponses(user.getPosts())).thenReturn(List.of(postResponse));

        List<PostResponse> result = userService.getUserPostsById(1L);

        assertEquals(1, result.size());
        assertEquals("Item", result.get(0).productName());

        verify(userRepository).findById(1L);
        verify(userMapper).toPostResponses(user.getPosts());
    }

    @Test
    void shouldReturnEmptyPosts_WhenUserHasNoPosts() {
        User user = new User();
        user.setPosts(List.of());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toPostResponses(List.of())).thenReturn(List.of());

        List<PostResponse> result = userService.getUserPostsById(1L);

        assertTrue(result.isEmpty());
    }

    // =========================
    // UPDATE USER
    // =========================
    @Test
    void shouldUpdateUserFromDto() {
        User user = new User();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserUpdateDTO dto = new UserUpdateDTO("new_user", "new@mail.com");

        userService.updateUserFromDto(1L, dto);

        verify(userRepository).findById(1L);
        verify(userMapper).updateUserFromDto(dto, user);
    }

    @Test
    void shouldThrow_WhenUpdatingNonExistingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        UserUpdateDTO dto = new UserUpdateDTO("new_user", "new@mail.com");

        assertThrows(NoSuchElementException.class,
                () -> userService.updateUserFromDto(1L, dto));

        verify(userMapper, never()).updateUserFromDto(any(), any());
    }

    // =========================
    // DELETE USER
    // =========================
    @Test
    void shouldDeleteUserById() {
        userService.deleteUserById(1L);

        verify(userRepository).deleteById(1L);
        verifyNoMoreInteractions(userRepository);
    }
}
