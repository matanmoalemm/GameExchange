package com.example.Post;

import com.example.user.User;
import com.example.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;


@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    public PostService(PostRepository postRepository, UserRepository userRepository, PostMapper postMapper) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postMapper = postMapper;
    }
    public User getUserById(Integer id){
        return userRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("User id : " + id + "was not found")
        );
    }

    public List<PostResponse> getPosts() {

        return postRepository.findAll().stream()
                .map(postMapper::toResponse)
                .toList();
    }

    public Post getPostById(Integer id) {
        return postRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("Post not found")
        );

    }

    public PostResponse getPostResponseById(Integer id){
        return postMapper.toResponse(getPostById(id));
    }


    @Transactional
    public void insertPost(PostRequest postRequest) {
        Post post = postMapper.toEntity(postRequest);
        postRepository.save(post);
    }
    @Transactional
    public void deletePostById(Integer id) {
        postRepository.deleteById(id);
    }

    @Transactional
    public void updatePostFromDto(Integer id,PostUpdateDto postUpdateDto){
        Post post = getPostById(id);
        postMapper.updatePostFromDto(postUpdateDto,post);
    }
}
