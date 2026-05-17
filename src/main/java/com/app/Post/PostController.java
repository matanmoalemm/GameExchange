package com.app.Post;

import com.app.Security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @ResponseStatus(HttpStatus.OK)
    public List<PostResponse> getLists(){

        return postService.getPosts();
    }
    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public PostResponse getPostById(@PathVariable Long id){

        return postService.getPostResponseById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse addNewPost(@AuthenticationPrincipal UserPrincipal principal,
                                   @RequestBody @Valid PostRequest postRequest) {
        return postService.insertPost(postRequest, principal);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@AuthenticationPrincipal UserPrincipal principal,
                           @PathVariable Long id) {
        postService.deletePostById(id, principal.getId());
    }

    @PatchMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePostFromDto(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody PostUpdateDto postUpdateDto) {
        postService.updatePostFromDto(id, postUpdateDto, principal.getId());
    }





}
