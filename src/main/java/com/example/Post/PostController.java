package com.example.Post;

import jakarta.validation.Valid;

import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.FOUND)
    public List<PostResponse> getLists(){

        return postService.getPosts();
    }
    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.FOUND)
    public PostResponse getPostById(@PathVariable Integer id){

        return postService.getPostResponseById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addNewPost(
            @RequestBody @Valid PostRequest postRequest
    ){
        postService.insertPost(postRequest);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @PathVariable Integer id
    ){
        postService.deletePostById(id);
    }

    @PatchMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePostFromDto(
                @PathVariable Integer id,
                @Valid @RequestBody PostUpdateDto postUpdateDto

    ){
        postService.updatePostFromDto(id, postUpdateDto);
    }





}
