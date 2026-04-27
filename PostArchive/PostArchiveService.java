package com.example.PostArchive;

import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PostArchiveService {
    private final PostArchiveRepository postArchiveRepository;

    public PostArchiveService(PostArchiveRepository postArchiveRepository) {
        this.postArchiveRepository = postArchiveRepository;
    }

    public List<PostArchive> getPosts() {
        return postArchiveRepository.findAll();
    }


    public PostArchive getPostById(String id) {
        return postArchiveRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException(id + " number not found")
        );
    }


    public void insertPost(@Valid PostArchive post) {
        postArchiveRepository.save(post);
    }

    public void deletePostById(String id) {
        postArchiveRepository.deleteById(id);
    }
}
