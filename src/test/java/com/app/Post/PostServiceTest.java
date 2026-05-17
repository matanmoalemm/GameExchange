package com.app.Post;

import com.app.Security.UserPrincipal;
import com.app.user.User;
import com.app.user.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    // =========================
    // ✅ HAPPY PATH
    // =========================
    @Nested
    class HappyPath {

        @Test
        void shouldReturnUser_WhenUserExists() {
            User user = new User();
            user.setId(1L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            User result = postService.getUserById(1L);

            assertSame(user, result);
            verify(userRepository).findById(1L);
        }

        @Test
        void shouldReturnMappedPosts() {
            Post post = new Post();
            PostResponse response = mock(PostResponse.class);

            when(postRepository.findAll()).thenReturn(List.of(post));
            when(postMapper.toResponse(post)).thenReturn(response);

            List<PostResponse> result = postService.getPosts();

            assertEquals(1, result.size());
            assertSame(response, result.get(0));

            verify(postMapper).toResponse(post);
            verify(postRepository).findAll();
        }

        @Test
        void shouldUpdatePost_WhenOwner() {
            User owner = new User();
            owner.setId(1L);
            Post post = new Post();
            post.setUser(owner);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            PostUpdateDto dto = new PostUpdateDto("Name", "Desc", 10, "http://url.com/img.jpg", "ACTIVE");

            postService.updatePostFromDto(1L, dto, 1L);

            verify(postMapper).updatePostFromDto(dto, post);
        }

        @Test
        void shouldDeletePost_WhenOwner() {
            User owner = new User();
            owner.setId(1L);
            Post post = new Post();
            post.setUser(owner);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            postService.deletePostById(1L, 1L);

            verify(postRepository).deleteById(1L);
        }

        @Test
        void shouldInsertPost() {
            PostRequest request = new PostRequest("Game", 50, "http://url.com/img.jpg", "desc");
            Post mappedPost = new Post();
            User user = new User();
            UserPrincipal principal = new UserPrincipal(1L, "test@test.com", List.of());
            PostResponse response = mock(PostResponse.class);

            when(postMapper.toEntity(request)).thenReturn(mappedPost);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(postRepository.save(mappedPost)).thenReturn(mappedPost);
            when(postMapper.toResponse(mappedPost)).thenReturn(response);

            PostResponse result = postService.insertPost(request, principal);

            assertSame(response, result);
            verify(postMapper).toEntity(request);
            verify(postRepository).save(mappedPost);
            verify(userRepository).findById(1L);
        }
    }

    // =========================
    // ✅ BOUNDARY
    // =========================
    @Nested
    class Boundary {

        @Test
        void shouldReturnEmptyList_WhenNoPostsExist() {
            when(postRepository.findAll()).thenReturn(Collections.emptyList());

            List<PostResponse> result = postService.getPosts();

            assertTrue(result.isEmpty());
            verify(postMapper, never()).toResponse(any());
        }

        @Test
        void shouldHandleLargeList() {
            List<Post> posts = Collections.nCopies(100, new Post());

            when(postRepository.findAll()).thenReturn(posts);
            when(postMapper.toResponse(any())).thenReturn(mock(PostResponse.class));

            List<PostResponse> result = postService.getPosts();

            assertEquals(100, result.size());
            verify(postMapper, times(100)).toResponse(any());
        }
    }

    // =========================
    // ❌ ERROR CASES
    // =========================
    @Nested
    class ErrorCases {

        @Test
        void shouldThrow_WhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> postService.getUserById(1L));
        }

        @Test
        void shouldThrow_WhenPostNotFound_OnUpdate() {
            when(postRepository.findById(1L)).thenReturn(Optional.empty());

            PostUpdateDto dto = new PostUpdateDto("Name", "Desc", 10, "http://url.com/img.jpg", "ACTIVE");

            assertThrows(NoSuchElementException.class,
                    () -> postService.updatePostFromDto(1L, dto, 1L));

            verify(postMapper, never()).updatePostFromDto(any(), any());
        }

        @Test
        void shouldThrow_WhenPostNotFound_OnDelete() {
            when(postRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> postService.deletePostById(1L, 1L));

            verify(postRepository, never()).deleteById(any());
        }

        @Test
        void shouldThrow_WhenUpdatingPostNotOwned() {
            User owner = new User();
            owner.setId(2L); // post belongs to user 2
            Post post = new Post();
            post.setUser(owner);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            PostUpdateDto dto = new PostUpdateDto("Name", "Desc", 10, "http://url.com/img.jpg", "ACTIVE");

            assertThrows(AccessDeniedException.class,
                    () -> postService.updatePostFromDto(1L, dto, 1L)); // requester is user 1

            verify(postMapper, never()).updatePostFromDto(any(), any());
        }

        @Test
        void shouldThrow_WhenDeletingPostNotOwned() {
            User owner = new User();
            owner.setId(2L); // post belongs to user 2
            Post post = new Post();
            post.setUser(owner);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThrows(AccessDeniedException.class,
                    () -> postService.deletePostById(1L, 1L)); // requester is user 1

            verify(postRepository, never()).deleteById(any());
        }

        @Test
        void shouldThrow_WhenGetPostResponseFails() {
            when(postRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> postService.getPostResponseById(1L));
        }
    }

    // =========================
    // 🔍 INTERACTION TESTS
    // =========================
    @Nested
    class InteractionTests {

        @Test
        void shouldMapPostResponseById() {
            Post post = new Post();
            PostResponse response = mock(PostResponse.class);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postMapper.toResponse(post)).thenReturn(response);

            PostResponse result = postService.getPostResponseById(1L);

            assertNotNull(result);
            verify(postRepository).findById(1L);
            verify(postMapper).toResponse(post);
        }
    }
}
