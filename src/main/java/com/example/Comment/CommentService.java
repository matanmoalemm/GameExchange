package com.example.Comment;

import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;


@Service
public class CommentService {
    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public List<Comment> getComments() {
        return commentRepository.findAll();
    }


    public Comment getCommentById(Integer id) {
        return commentRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException(id + " number not found")
        );
    }


    public void insertComment(@Valid Comment comment) {

        commentRepository.save(comment);
    }


    public void deleteCommentById(Integer id) {
        commentRepository.deleteById(id);
    }
}
