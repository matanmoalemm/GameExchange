package com.example.Comment;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }


    @GetMapping
    public List<Comment> getLists(){

        return commentService.getComments();
    }
    @GetMapping("{id}")
    public Comment getCommentById(@PathVariable Integer id){

        return commentService.getCommentById(id);
    }
    @PostMapping
    public void addNewComment(
            @RequestBody @Valid Comment comment
    ){
        commentService.insertComment(comment);
    }

    @DeleteMapping("{id}")
    public void deletePost(
            @PathVariable Integer id
    ){
        commentService.deleteCommentById(id);
    }
}
