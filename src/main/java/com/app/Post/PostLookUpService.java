package com.app.Post;

import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class PostLookUpService {
    private final PostRepository postRepository;

    public PostLookUpService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Post not found"));
    }
}
