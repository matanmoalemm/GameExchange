package com.app.Comment;

import com.app.Post.Post;
import com.app.Post.PostLookUpService;
import com.app.Security.UserPrincipal;
import com.app.User.User;
import com.app.User.UserLookupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Unit Tests")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private PostLookUpService postLookUpService;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    @Nested
    @DisplayName("getCommentsByPostId")
    class GetCommentsByPostId {

        @Test
        @DisplayName("should return comments when post exists")
        void shouldReturnComments_WhenPostExists() {
            Post post = new Post();
            Comment comment = new Comment();
            CommentResponse response = new CommentResponse(1L, "Nice!", 1L, 2L, LocalDateTime.now());

            when(postLookUpService.getPostById(1L)).thenReturn(post);
            when(commentRepository.findByPostId(1L)).thenReturn(List.of(comment));
            when(commentMapper.toResponse(comment)).thenReturn(response);

            List<CommentResponse> result = commentService.getCommentsByPostId(1L);

            assertEquals(1, result.size());
            assertSame(response, result.get(0));
        }

        @Test
        @DisplayName("should return empty list when post has no comments")
        void shouldReturnEmptyList_WhenPostHasNoComments() {
            Post post = new Post();

            when(postLookUpService.getPostById(1L)).thenReturn(post);
            when(commentRepository.findByPostId(1L)).thenReturn(List.of());

            List<CommentResponse> result = commentService.getCommentsByPostId(1L);

            assertTrue(result.isEmpty());
            verify(commentMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("should throw NoSuchElementException when post is not found")
        void shouldThrow_WhenPostNotFound() {
            when(postLookUpService.getPostById(1L)).thenThrow(new NoSuchElementException("Post not found"));

            assertThrows(NoSuchElementException.class, () -> commentService.getCommentsByPostId(1L));
            verify(commentRepository, never()).findByPostId(any());
        }

        @Test
        @DisplayName("should return comments for any authenticated user — no ownership check")
        void shouldReturnComments_WhenUserIsNotPostOwner() {
            Post post = new Post();
            Comment comment = new Comment();
            CommentResponse response = new CommentResponse(1L, "Nice!", 1L, 2L, LocalDateTime.now());

            when(postLookUpService.getPostById(1L)).thenReturn(post);
            when(commentRepository.findByPostId(1L)).thenReturn(List.of(comment));
            when(commentMapper.toResponse(comment)).thenReturn(response);

            List<CommentResponse> result = commentService.getCommentsByPostId(1L);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("should handle large number of comments without degradation")
        void shouldHandleLargeNumberOfComments() {
            Post post = new Post();
            List<Comment> comments = Collections.nCopies(1000, new Comment());

            when(postLookUpService.getPostById(1L)).thenReturn(post);
            when(commentRepository.findByPostId(1L)).thenReturn(comments);
            when(commentMapper.toResponse(any())).thenReturn(mock(CommentResponse.class));

            List<CommentResponse> result = commentService.getCommentsByPostId(1L);

            assertEquals(1000, result.size());
            verify(commentMapper, times(1000)).toResponse(any());
        }
    }

    @Nested
    @DisplayName("insertComment")
    class InsertComment {

        @Test
        @DisplayName("should insert comment with correct fields when all inputs are valid")
        void shouldInsertComment_WhenValid() {
            CommentRequest request = new CommentRequest(1L, "Great game!");
            UserPrincipal principal = new UserPrincipal(1L, "user@test.com", List.of());
            User user = new User();
            user.setId(1L);
            Post post = new Post();
            post.setId(1L);

            when(userLookupService.getById(1L)).thenReturn(user);
            when(postLookUpService.getPostById(1L)).thenReturn(post);
            when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(commentMapper.toResponse(any())).thenReturn(mock(CommentResponse.class));

            commentService.insertComment(request, principal);

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertSame(user, captor.getValue().getUser());
            assertSame(post, captor.getValue().getPost());
            assertEquals("Great game!", captor.getValue().getText());
        }

        @Test
        @DisplayName("should throw NullPointerException when UserPrincipal is null")
        void shouldThrow_WhenPrincipalIsNull() {
            CommentRequest request = new CommentRequest(1L, "Nice!");

            assertThrows(NullPointerException.class, () -> commentService.insertComment(request, null));
            verify(userLookupService, never()).getById(any());
        }

        @Test
        @DisplayName("should throw NullPointerException when CommentRequest is null")
        void shouldThrow_WhenRequestIsNull() {
            UserPrincipal principal = new UserPrincipal(1L, "user@test.com", List.of());
            User user = new User();

            when(userLookupService.getById(1L)).thenReturn(user);

            assertThrows(NullPointerException.class, () -> commentService.insertComment(null, principal));
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw NoSuchElementException when user is not found")
        void shouldThrow_WhenUserNotFound() {
            CommentRequest request = new CommentRequest(1L, "Nice!");
            UserPrincipal principal = new UserPrincipal(1L, "user@test.com", List.of());

            when(userLookupService.getById(1L)).thenThrow(new NoSuchElementException("User not found"));

            assertThrows(NoSuchElementException.class, () -> commentService.insertComment(request, principal));
            verify(postLookUpService, never()).getPostById(any());
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw NoSuchElementException when post is not found")
        void shouldThrow_WhenPostNotFound() {
            CommentRequest request = new CommentRequest(1L, "Nice!");
            UserPrincipal principal = new UserPrincipal(1L, "user@test.com", List.of());
            User user = new User();

            when(userLookupService.getById(1L)).thenReturn(user);
            when(postLookUpService.getPostById(1L)).thenThrow(new NoSuchElementException("Post not found"));

            assertThrows(NoSuchElementException.class, () -> commentService.insertComment(request, principal));
            verify(commentRepository, never()).save(any());
        }

    }

    @Nested
    @DisplayName("deleteCommentById")
    class DeleteCommentById {

        @Test
        @DisplayName("should delete comment when requester is the author")
        void shouldDeleteComment_WhenAuthor() {
            User author = new User();
            author.setId(1L);
            Comment comment = new Comment();
            comment.setUser(author);

            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            commentService.deleteCommentById(1L, 1L);

            verify(commentRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when comment is not found")
        void shouldThrow_WhenCommentNotFound() {
            when(commentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () -> commentService.deleteCommentById(1L, 1L));
            verify(commentRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when requester is not the author")
        void shouldThrow_WhenRequesterIsNotAuthor() {
            User author = new User();
            author.setId(2L);
            Comment comment = new Comment();
            comment.setUser(author);

            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            assertThrows(AccessDeniedException.class, () -> commentService.deleteCommentById(1L, 1L));
            verify(commentRepository, never()).deleteById(any());
        }

    }
}
