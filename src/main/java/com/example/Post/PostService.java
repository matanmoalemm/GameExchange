package com.example.Post;

import com.example.PostArchive.PostArchive;
import com.example.PostArchive.PostArchiveRepository;
import com.example.PostArchive.Reason;
import com.example.user.User;
import com.example.user.UserController;
import com.example.user.UserRepository;
import com.example.user.UserService;
import jakarta.transaction.Transactional;
import org.hibernate.validator.constraints.URL;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;


@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostArchiveRepository postArchiveRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository, PostArchiveRepository postArchiveRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postArchiveRepository = postArchiveRepository;
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

    public List<Post> getPosts() {

        return postRepository.findAll();
    }

    public Post getPostById(Integer id) {
        return postRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException(id + " not found")
        );

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
    public void markAsSold(Integer id,Integer userId) {
        Post post = getPostById(id);
        User user = getUserById(id);

        PostArchive postArchive = new PostArchive(post, Reason.SOLD,userId,user.getUsername());
        postRepository.deleteById(id);
        postArchiveRepository.save(postArchive);

    }
}
