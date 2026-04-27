package com.example.PostArchive;

import com.example.Post.Post;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/posts_archive")
public class PostArchiveController {

    private final PostArchiveService postArchiveService;


    public PostArchiveController(PostArchiveService postArchiveService) {
        this.postArchiveService = postArchiveService;
    }
    @GetMapping
    public List<PostArchive> getLists(){

        return postArchiveService.getPosts();
    }
    @GetMapping("{id}")
    public PostArchive getPostById(@PathVariable String id){

        return postArchiveService.getPostById(id);
    }
    @PostMapping
    public void addNewPost(
            @RequestBody @Valid PostArchive post
    ){
        postArchiveService.insertPost(post);
    }

    @DeleteMapping("{id}")
    public void deletePost(
            @PathVariable String id
    ){
        postArchiveService.deletePostById(id);
    }

}
