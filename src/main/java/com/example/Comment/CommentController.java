package com.example.Comment;

import com.example.Security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/post/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentResponse> getCommentsByPost(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long postId) {
        return commentService.getCommentsByPostId(postId, principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addNewComment(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestBody @Valid CommentRequest request) {
        return commentService.insertComment(request, principal);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal UserPrincipal principal,
                              @PathVariable Long id) {
        commentService.deleteCommentById(id, principal.getId());
    }
}
