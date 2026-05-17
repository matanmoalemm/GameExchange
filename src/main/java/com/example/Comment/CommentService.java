package com.example.Comment;

import com.example.Post.Post;
import com.example.Post.PostRepository;
import com.example.Security.UserPrincipal;
import com.example.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;


@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentMapper commentMapper;

    public CommentService(CommentRepository commentRepository,
                          UserRepository userRepository,
                          PostRepository postRepository,
                          CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentMapper = commentMapper;
    }

    public List<CommentResponse> getCommentsByPostId(Long postId, Long requesterId) {
        Post post = getPostById(postId);
        if (!post.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("Only the post owner can view comments");
        }
        return commentRepository.findByPostId(postId).stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse insertComment(CommentRequest request, UserPrincipal principal) {
        Comment comment = new Comment();
        comment.setUser(userRepository.findById(principal.getId())
                .orElseThrow(() -> new NoSuchElementException("User not found")));
        comment.setPost(getPostById(request.postId()));
        comment.setText(request.text());
        return commentMapper.toResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteCommentById(Long id, Long requesterId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Comment not found: " + id));
        if (!comment.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("You do not own this comment");
        }
        commentRepository.deleteById(id);
    }

    private Post getPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + postId));
    }
}
