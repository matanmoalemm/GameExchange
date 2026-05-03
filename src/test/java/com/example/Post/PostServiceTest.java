package com.example.Post;

import com.example.user.User;
import com.example.user.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
            user.setId(1);

            when(userRepository.findById(1)).thenReturn(Optional.of(user));

            User result = postService.getUserById(1);

            assertSame(user, result);
            verify(userRepository).findById(1);
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
        void shouldUpdatePost_WhenPostExists() {
            Post post = new Post();
            when(postRepository.findById(1)).thenReturn(Optional.of(post));

            PostUpdateDto dto = new PostUpdateDto("Name", "Desc", 10, "url", "ACTIVE");

            postService.updatePostFromDto(1, dto);

            verify(postMapper).updatePostFromDto(dto, post);
        }

        @Test
        void shouldInsertPost() {
            PostRequest request = new PostRequest("Game", 50, "url", 1, "desc");
            Post mappedPost = new Post();
            User user = new User();


            when(postMapper.toEntity(request)).thenReturn(mappedPost);
            when(userRepository.findById(1)).thenReturn(Optional.of(user));

            postService.insertPost(request);

            verify(postMapper).toEntity(request);
            verify(postRepository).save(mappedPost);
            verify(userRepository).findById(1);
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
            when(userRepository.findById(1)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> postService.getUserById(1));
        }

        @Test
        void shouldThrow_WhenPostNotFound_OnUpdate() {
            when(postRepository.findById(1)).thenReturn(Optional.empty());

            PostUpdateDto dto = new PostUpdateDto("Name", "Desc", 10, "url", "ACTIVE");

            assertThrows(NoSuchElementException.class,
                    () -> postService.updatePostFromDto(1, dto));

            verify(postMapper, never()).updatePostFromDto(any(), any());
        }

        @Test
        void shouldThrow_WhenGetPostResponseFails() {
            when(postRepository.findById(1)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> postService.getPostResponseById(1));
        }
    }

    // =========================
    // 🔍 INTERACTION TESTS
    // =========================
    @Nested
    class InteractionTests {

        @Test
        void shouldDeletePost() {
            postService.deletePostById(1);

            verify(postRepository).deleteById(1);
            verifyNoMoreInteractions(postRepository);
        }

        @Test
        void shouldMapPostResponseById() {
            Post post = new Post();
            PostResponse response = mock(PostResponse.class);

            when(postRepository.findById(1)).thenReturn(Optional.of(post));
            when(postMapper.toResponse(post)).thenReturn(response);

            PostResponse result = postService.getPostResponseById(1);

            assertNotNull(result);
            verify(postRepository).findById(1);
            verify(postMapper).toResponse(post);
        }
    }
}