package com.app.Post;

import com.app.Security.UserPrincipal;
import com.app.User.User;
import com.app.User.UserLookupService;
import org.junit.jupiter.api.DisplayName;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService Unit Tests")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLookUpService postLookUpService;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    @Nested
    @DisplayName("getPosts")
    class GetPosts {

        @Test
        @DisplayName("should return all posts as mapped responses")
        void shouldReturnAllPostsAsMappedResponses() {
            Post post = new Post();
            PostResponse response = mock(PostResponse.class);

            when(postRepository.findAll()).thenReturn(List.of(post));
            when(postMapper.toResponse(post)).thenReturn(response);

            List<PostResponse> result = postService.getPosts();

            assertEquals(1, result.size());
            assertSame(response, result.get(0));
            verify(postRepository).findAll();
            verify(postMapper).toResponse(post);
        }

        @Test
        @DisplayName("should return empty list when no posts exist")
        void shouldReturnEmptyList_WhenNoPostsExist() {
            when(postRepository.findAll()).thenReturn(Collections.emptyList());

            List<PostResponse> result = postService.getPosts();

            assertTrue(result.isEmpty());
            verify(postMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("should handle large number of posts without degradation")
        void shouldHandleLargeNumberOfPosts() {
            List<Post> posts = Collections.nCopies(1000, new Post());

            when(postRepository.findAll()).thenReturn(posts);
            when(postMapper.toResponse(any())).thenReturn(mock(PostResponse.class));

            List<PostResponse> result = postService.getPosts();

            assertEquals(1000, result.size());
            verify(postMapper, times(1000)).toResponse(any());
        }
    }

    @Nested
    @DisplayName("getPostById")
    class GetPostById {

        @Test
        @DisplayName("should return post when valid ID is provided")
        void shouldReturnPost_WhenValidIdProvided() {
            Post post = new Post();
            when(postLookUpService.getPostById(1L)).thenReturn(post);

            Post result = postService.getPostById(1L);

            assertSame(post, result);
            verify(postLookUpService).getPostById(1L);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when post is not found")
        void shouldThrow_WhenPostNotFound() {
            when(postLookUpService.getPostById(1L)).thenThrow(new NoSuchElementException("Post not found"));

            assertThrows(NoSuchElementException.class, () -> postService.getPostById(1L));
        }

        @Test
        @DisplayName("should throw NoSuchElementException when ID is null")
        void shouldThrow_WhenIdIsNull() {
            when(postLookUpService.getPostById(null)).thenThrow(new NoSuchElementException("Post not found"));

            assertThrows(NoSuchElementException.class, () -> postService.getPostById(null));
            verify(postLookUpService).getPostById(null);
        }
    }

    @Nested
    @DisplayName("getPostResponseById")
    class GetPostResponseById {

        @Test
        @DisplayName("should return PostResponse when valid ID is provided")
        void shouldReturnPostResponse_WhenValidIdProvided() {
            Post post = new Post();
            PostResponse response = mock(PostResponse.class);

            when(postLookUpService.getPostById(1L)).thenReturn(post);
            when(postMapper.toResponse(post)).thenReturn(response);

            PostResponse result = postService.getPostResponseById(1L);

            assertSame(response, result);
            verify(postLookUpService).getPostById(1L);
            verify(postMapper).toResponse(post);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when post is not found")
        void shouldThrow_WhenPostNotFound() {
            when(postLookUpService.getPostById(1L)).thenThrow(new NoSuchElementException("Post not found"));

            assertThrows(NoSuchElementException.class, () -> postService.getPostResponseById(1L));
        }

        @Test
        @DisplayName("should throw NoSuchElementException when ID is null")
        void shouldThrow_WhenIdIsNull() {
            when(postLookUpService.getPostById(null)).thenThrow(new NoSuchElementException("Post not found"));

            assertThrows(NoSuchElementException.class, () -> postService.getPostResponseById(null));
        }
    }

    @Nested
    @DisplayName("insertPost")
    class InsertPost {

        @Test
        @DisplayName("should insert post when all inputs are valid")
        void shouldInsertPost_WhenInputsAreValid() {
            PostRequest request = new PostRequest("Game", 50, "http://url.com/img.jpg", "desc");
            Post mappedPost = new Post();
            User user = new User();
            UserPrincipal principal = new UserPrincipal(1L, "test@test.com", List.of());
            PostResponse response = mock(PostResponse.class);

            when(postMapper.toEntity(request)).thenReturn(mappedPost);
            when(userLookupService.getById(1L)).thenReturn(user);
            when(postRepository.save(mappedPost)).thenReturn(mappedPost);
            when(postMapper.toResponse(mappedPost)).thenReturn(response);

            PostResponse result = postService.insertPost(request, principal);

            assertSame(response, result);
            verify(postMapper).toEntity(request);
            verify(userLookupService).getById(1L);
            verify(postRepository).save(mappedPost);
        }

        @Test
        @DisplayName("should throw NullPointerException when PostRequest is null")
        void shouldThrow_WhenPostRequestIsNull() {
            UserPrincipal principal = new UserPrincipal(1L, "test@test.com", List.of());

            assertThrows(NullPointerException.class, () -> postService.insertPost(null, principal));
        }

        @Test
        @DisplayName("should throw NullPointerException when UserPrincipal is null")
        void shouldThrow_WhenPrincipalIsNull() {
            PostRequest request = new PostRequest("Game", 50, "http://url.com/img.jpg", "desc");
            Post mappedPost = new Post();
            when(postMapper.toEntity(request)).thenReturn(mappedPost);

            assertThrows(NullPointerException.class, () -> postService.insertPost(request, null));
        }

        @Test
        @DisplayName("should throw NoSuchElementException when user does not exist")
        void shouldThrow_WhenUserNotFound() {
            PostRequest request = new PostRequest("Game", 50, "http://url.com/img.jpg", "desc");
            Post mappedPost = new Post();
            UserPrincipal principal = new UserPrincipal(1L, "test@test.com", List.of());

            when(postMapper.toEntity(request)).thenReturn(mappedPost);
            when(userLookupService.getById(1L)).thenThrow(new NoSuchElementException("User not found"));

            assertThrows(NoSuchElementException.class, () -> postService.insertPost(request, principal));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("should pass blank product name through to mapper — validation is at the controller")
        void shouldPassThrough_WhenProductNameIsBlank() {
            PostRequest request = new PostRequest("", 50, "http://url.com/img.jpg", "desc");
            Post mappedPost = new Post();
            User user = new User();
            UserPrincipal principal = new UserPrincipal(1L, "test@test.com", List.of());
            PostResponse response = mock(PostResponse.class);

            when(postMapper.toEntity(request)).thenReturn(mappedPost);
            when(userLookupService.getById(1L)).thenReturn(user);
            when(postRepository.save(mappedPost)).thenReturn(mappedPost);
            when(postMapper.toResponse(mappedPost)).thenReturn(response);

            assertDoesNotThrow(() -> postService.insertPost(request, principal));
        }
    }

    @Nested
    @DisplayName("deletePostById")
    class DeletePostById {

        @Test
        @DisplayName("should delete post when requester is the owner")
        void shouldDeletePost_WhenOwner() {
            User owner = new User();
            owner.setId(1L);
            Post post = new Post();
            post.setUser(owner);

            when(postLookUpService.getPostById(1L)).thenReturn(post);

            postService.deletePostById(1L, 1L);

            verify(postRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when post is not found")
        void shouldThrow_WhenPostNotFound() {
            when(postLookUpService.getPostById(1L)).thenThrow(new NoSuchElementException("Post not found"));

            assertThrows(NoSuchElementException.class, () -> postService.deletePostById(1L, 1L));
            verify(postRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when requester is not the owner")
        void shouldThrow_WhenRequesterIsNotOwner() {
            User owner = new User();
            owner.setId(2L);
            Post post = new Post();
            post.setUser(owner);

            when(postLookUpService.getPostById(1L)).thenReturn(post);

            assertThrows(AccessDeniedException.class, () -> postService.deletePostById(1L, 1L));
            verify(postRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should throw NoSuchElementException when ID is null")
        void shouldThrow_WhenIdIsNull() {
            when(postLookUpService.getPostById(null)).thenThrow(new NoSuchElementException("Post not found"));

            assertThrows(NoSuchElementException.class, () -> postService.deletePostById(null, 1L));
        }
    }

    @Nested
    @DisplayName("updatePostFromDto")
    class UpdatePostFromDto {

        @Test
        @DisplayName("should update post when requester is the owner and DTO is valid")
        void shouldUpdatePost_WhenOwnerAndValidDto() {
            User owner = new User();
            owner.setId(1L);
            Post post = new Post();
            post.setUser(owner);
            PostUpdateDto dto = new PostUpdateDto("Name", "Desc", 10, "http://url.com/img.jpg", "ACTIVE");

            when(postLookUpService.getPostById(1L)).thenReturn(post);

            postService.updatePostFromDto(1L, dto, 1L);

            verify(postMapper).updatePostFromDto(dto, post);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when post is not found")
        void shouldThrow_WhenPostNotFound() {
            when(postLookUpService.getPostById(1L)).thenThrow(new NoSuchElementException("Post not found"));
            PostUpdateDto dto = new PostUpdateDto("Name", "Desc", 10, "http://url.com/img.jpg", "ACTIVE");

            assertThrows(NoSuchElementException.class, () -> postService.updatePostFromDto(1L, dto, 1L));
            verify(postMapper, never()).updatePostFromDto(any(), any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when requester is not the owner")
        void shouldThrow_WhenRequesterIsNotOwner() {
            User owner = new User();
            owner.setId(2L);
            Post post = new Post();
            post.setUser(owner);
            PostUpdateDto dto = new PostUpdateDto("Name", "Desc", 10, "http://url.com/img.jpg", "ACTIVE");

            when(postLookUpService.getPostById(1L)).thenReturn(post);

            assertThrows(AccessDeniedException.class, () -> postService.updatePostFromDto(1L, dto, 1L));
            verify(postMapper, never()).updatePostFromDto(any(), any());
        }

        @Test
        @DisplayName("should throw NoSuchElementException when ID is null")
        void shouldThrow_WhenIdIsNull() {
            when(postLookUpService.getPostById(null)).thenThrow(new NoSuchElementException("Post not found"));
            PostUpdateDto dto = new PostUpdateDto("Name", "Desc", 10, "http://url.com/img.jpg", "ACTIVE");

            assertThrows(NoSuchElementException.class, () -> postService.updatePostFromDto(null, dto, 1L));
        }

        @Test
        @DisplayName("should pass null DTO fields to mapper — MapStruct IGNORE strategy handles them")
        void shouldPassNullDtoFieldsToMapper() {
            User owner = new User();
            owner.setId(1L);
            Post post = new Post();
            post.setUser(owner);
            PostUpdateDto dto = new PostUpdateDto(null, null, null, null, null);

            when(postLookUpService.getPostById(1L)).thenReturn(post);

            postService.updatePostFromDto(1L, dto, 1L);

            verify(postMapper).updatePostFromDto(dto, post);
        }
    }
}
