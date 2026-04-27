package com.example.Post;

import com.example.user.User;
import com.example.user.UserRepository;
import jakarta.transaction.Transactional;
import org.hibernate.validator.constraints.URL;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;


@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }
    public User getUserById(Integer id){
        return userRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("User id : " + id + "was not found")
        );
    }

    public Post createPost(PostRequest postRequest){
        Post post = new Post();
        post.setDescription(postRequest.description());
        post.setPrice(postRequest.price());
        post.setProductName(postRequest.productName());
        post.setPicUrl(postRequest.picUrl());
        post.setUser(getUserById(postRequest.userId()));
        return post;
    }

    public PostResponse createPostResponse(Post post){
        return new PostResponse(
                post.getId(),
                post.getProductName(),
                post.getDescription(),
                post.getPrice(),
                post.getPicUrl(),
                post.getUser().getId(),
                post.getStatus(),
                post.getCreatedAt());
    }

    public List<PostResponse> getPosts() {

        return postRepository.findAll().stream()
                .map(this::createPostResponse)
                .toList();
    }

    public Post getPostById(Integer id) {
        return postRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException(id + " not found")
        );

    }

    public PostResponse getPostResponseById(Integer id){
        return createPostResponse(getPostById(id));
    }


    @Transactional
    public void insertPost(PostRequest postRequest) {
        Post post = createPost(postRequest);
        postRepository.save(post);
    }
    @Transactional
    public void deletePostById(Integer id) {
        postRepository.deleteById(id);
    }

    @Transactional
    public void markAsPending(Integer id) {
        Post post = getPostById(id);
        post.setStatus(PostStatus.PENDING);
    }
    @Transactional
    public void updatePicUrl(Integer id, @URL String picURL) {
        Post post = getPostById(id);
        post.setPicUrl(picURL);
    }
    @Transactional
    public void updatePrice(Integer id, Integer price) {
        Post post = getPostById(id);
        post.setPrice(price);
    }
    @Transactional
    public void updateDescription(Integer id, String description) {
        Post post = getPostById(id);
        post.setDescription(description);
    }
    @Transactional
    public void markAsSold(Integer id) {
        Post post = getPostById(id);
        post.setStatus(PostStatus.SOLD);
    }
}
