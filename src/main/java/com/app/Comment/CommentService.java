package com.app.Comment;
import com.app.Post.PostLookUpService;
import com.app.Security.UserPrincipal;
import com.app.User.UserLookupService;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;


@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserLookupService userLookupService;
    private final PostLookUpService postLookUpService;
    private final CommentMapper commentMapper;

    public CommentService(CommentRepository commentRepository,
                          UserLookupService userLookupService,
                          PostLookUpService postLookUpService,
                          CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.userLookupService = userLookupService;
        this.postLookUpService = postLookUpService;
        this.commentMapper = commentMapper;
    }

    public List<CommentResponse> getCommentsByPostId(Long postId) {
        postLookUpService.getPostById(postId);
        return commentRepository.findByPostId(postId).stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse insertComment(CommentRequest request, UserPrincipal principal) {
        Comment comment = new Comment();
        comment.setUser(userLookupService.getById(principal.getId()));
        comment.setPost(postLookUpService.getPostById(request.postId()));
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
}
