package com.example.Post;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.hibernate.validator.constraints.URL;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("api/v1/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public List<Post> getLists(){

        return postService.getPosts();
    }
    @GetMapping("{id}")
    public Post getPostById(@PathVariable Integer id){

        return postService.getPostById(id);
    }

    @PostMapping
    public void addNewPost(
            @RequestBody @Valid PostRequest postRequest
    ){
        postService.insertPost(postRequest);
    }

    @DeleteMapping("{id}")
    public void deletePost(
            @PathVariable Integer id
    ){
        postService.deletePostById(id);
    }

    @PutMapping("{id}/PENDING")
    public void markAsPending(@PathVariable Integer id){
        postService.markAsPending(id);
    }

    @PutMapping("{id}/picURL")
    public void updatePicUrl(@PathVariable Integer id, @RequestParam @URL String picURL){
        postService.updatePicUrl(id, picURL);
    }
    @PutMapping("{id}/price")
    public void updatePrice(@PathVariable Integer id, @RequestParam Integer price){
        postService.updatePrice(id, price);
    }

    @PutMapping("{id}/description")
    public void updatePrice(@PathVariable Integer id, @RequestBody String description){
        postService.updateDescription(id, description);
    }


    @PutMapping("{id}/SOLD")
    public void markAsSold(@PathVariable Integer id, @RequestParam Integer userId){
        postService.markAsSold(id,userId);
    }





}
