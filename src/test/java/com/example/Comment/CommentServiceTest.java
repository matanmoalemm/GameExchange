package com.example.Comment;

import com.example.Post.Post;
import com.example.Post.PostRepository;
import com.example.Security.UserPrincipal;
import com.example.user.User;
import com.example.user.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    // =========================
    // ✅ HAPPY PATH
    // =========================
    @Nested
    class HappyPath {

        @Test
        void shouldInsertComment() {
            CommentRequest request = new CommentRequest(1L, "Great game!");
            UserPrincipal principal = new UserPrincipal(1L, "user@test.com", List.of());

            User user = new User();
            user.setId(1L);
            Post post = new Post();
            post.setId(1L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
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
        void shouldReturnComments_WhenRequesterIsPostOwner() {
            User postOwner = new User();
            postOwner.setId(1L);
            Post post = new Post();
            post.setId(1L);
            post.setUser(postOwner);

            Comment comment = new Comment();
            CommentResponse response = new CommentResponse(1L, "Nice!", 1L, 2L, LocalDateTime.now());

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(commentRepository.findByPostId(1L)).thenReturn(List.of(comment));
            when(commentMapper.toResponse(comment)).thenReturn(response);

            List<CommentResponse> result = commentService.getCommentsByPostId(1L, 1L);

            assertEquals(1, result.size());
            assertSame(response, result.get(0));
        }

        @Test
        void shouldDeleteComment_WhenAuthor() {
            User author = new User();
            author.setId(1L);
            Comment comment = new Comment();
            comment.setUser(author);

            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            commentService.deleteCommentById(1L, 1L);

            verify(commentRepository).deleteById(1L);
        }
    }

    // =========================
    // ✅ BOUNDARY
    // =========================
    @Nested
    class Boundary {

        @Test
        void shouldReturnEmptyList_WhenPostHasNoComments() {
            User postOwner = new User();
            postOwner.setId(1L);
            Post post = new Post();
            post.setUser(postOwner);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(commentRepository.findByPostId(1L)).thenReturn(List.of());

            List<CommentResponse> result = commentService.getCommentsByPostId(1L, 1L);

            assertTrue(result.isEmpty());
            verify(commentMapper, never()).toResponse(any());
        }
    }

    // =========================
    // ❌ ERROR CASES
    // =========================
    @Nested
    class ErrorCases {

        @Test
        void shouldThrow_WhenUserNotFound_OnInsert() {
            CommentRequest request = new CommentRequest(1L, "Nice!");
            UserPrincipal principal = new UserPrincipal(1L, "user@test.com", List.of());

            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> commentService.insertComment(request, principal));

            verify(postRepository, never()).findById(any());
            verify(commentRepository, never()).save(any());
        }

        @Test
        void shouldThrow_WhenPostNotFound_OnInsert() {
            CommentRequest request = new CommentRequest(1L, "Nice!");
            UserPrincipal principal = new UserPrincipal(1L, "user@test.com", List.of());
            User user = new User();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(postRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> commentService.insertComment(request, principal));

            verify(commentRepository, never()).save(any());
        }

        @Test
        void shouldThrow_WhenCommentNotFound_OnDelete() {
            when(commentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> commentService.deleteCommentById(1L, 1L));

            verify(commentRepository, never()).deleteById(any());
        }

        @Test
        void shouldThrow_AccessDenied_WhenNonOwnerGetsComments() {
            User postOwner = new User();
            postOwner.setId(2L); // post belongs to user 2
            Post post = new Post();
            post.setUser(postOwner);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThrows(AccessDeniedException.class,
                    () -> commentService.getCommentsByPostId(1L, 1L)); // requester is user 1

            verify(commentRepository, never()).findByPostId(any());
        }

        @Test
        void shouldThrow_AccessDenied_WhenNonAuthorDeletesComment() {
            User author = new User();
            author.setId(2L); // comment written by user 2
            Comment comment = new Comment();
            comment.setUser(author);

            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            assertThrows(AccessDeniedException.class,
                    () -> commentService.deleteCommentById(1L, 1L)); // requester is user 1

            verify(commentRepository, never()).deleteById(any());
        }
    }
}
