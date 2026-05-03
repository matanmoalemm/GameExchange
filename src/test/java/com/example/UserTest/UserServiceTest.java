package com.example.UserTest;

import com.example.Post.Post;
import com.example.Post.PostMapper;
import com.example.Post.PostResponse;
import com.example.user.*;
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
        user.setId(1);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1);

        assertEquals(1, result.getId());
        verify(userRepository).findById(1);
    }

    @Test
    void shouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> userService.getUserById(1));

        verify(userRepository).findById(1);
    }

    // =========================
    // GET USERS
    // =========================
    @Test
    void shouldReturnAllUsers_AsResponses() {
        User user = new User();
        UserResponse response = new UserResponse(1, "john", List.of());

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        List<UserResponse> result = userService.getUsers();

        assertEquals(1, result.size());
        assertEquals("john", result.get(0).username());

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

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getUserResponseById(1);

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
                1, "Item", "Desc", 10, "url", 1, "ACTIVE", LocalDateTime.now()
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userMapper.toPostResponses(user.getPosts()))
                .thenReturn(List.of(postResponse));

        List<PostResponse> result = userService.getUserPostsById(1);

        assertEquals(1, result.size());
        assertEquals("Item", result.get(0).productName());

        verify(userRepository).findById(1);
        verify(userMapper).toPostResponses(user.getPosts());
    }

    @Test
    void shouldReturnEmptyPosts_WhenUserHasNoPosts() {
        User user = new User();
        user.setPosts(List.of());

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userMapper.toPostResponses(List.of())).thenReturn(List.of());

        List<PostResponse> result = userService.getUserPostsById(1);

        assertTrue(result.isEmpty());
    }

    // =========================
    // UPDATE USER
    // =========================
    @Test
    void shouldUpdateUserFromDto() {
        User user = new User();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UserUpdateDTO dto = new UserUpdateDTO("new_user", "new@mail.com");

        userService.updateUserFromDto(1, dto);

        verify(userRepository).findById(1);
        verify(userMapper).updateUserFromDto(dto, user);
    }

    @Test
    void shouldThrow_WhenUpdatingNonExistingUser() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        UserUpdateDTO dto = new UserUpdateDTO("x", "x");

        assertThrows(NoSuchElementException.class,
                () -> userService.updateUserFromDto(1, dto));

        verify(userMapper, never()).updateUserFromDto(any(), any());
    }

    // =========================
    // DELETE USER
    // =========================
    @Test
    void shouldDeleteUserById() {
        userService.deleteUserById(1);

        verify(userRepository).deleteById(1);
        verifyNoMoreInteractions(userRepository);
    }
}