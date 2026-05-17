package com.example.Post;

import com.example.Security.UserPrincipal;
import com.example.user.User;
import com.example.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
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


    public User getUserById(Long id){
        return userRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("User id : " + id + "was not found")
        );
    }

    public List<PostResponse> getPosts() {

        return postRepository.findAll().stream()
                .map(postMapper::toResponse)
                .toList();
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("Post not found")
        );

    }

    public PostResponse getPostResponseById(Long id){
        return postMapper.toResponse(getPostById(id));
    }


    @Transactional
    public PostResponse insertPost(PostRequest postRequest, UserPrincipal userPrincipal) {
        Post post = postMapper.toEntity(postRequest);
        post.setUser(getUserById(userPrincipal.getId()));
        return postMapper.toResponse(postRepository.save(post));
    }

    @Transactional
    public void deletePostById(Long id, Long requesterId) {
        Post post = getPostById(id);
        verifyOwnership(post, requesterId);
        postRepository.deleteById(id);
    }

    @Transactional
    public void updatePostFromDto(Long id, PostUpdateDto postUpdateDto, Long requesterId) {
        Post post = getPostById(id);
        verifyOwnership(post, requesterId);
        postMapper.updatePostFromDto(postUpdateDto, post);
    }

    private void verifyOwnership(Post post, Long requesterId) {
        if (!post.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("You do not own this post");
        }
    }
}
