package com.app.Post;

import com.app.Security.UserPrincipal;
import com.app.User.UserLookupService;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostLookUpService postLookUpService;
    private final UserLookupService userLookupService;
    private final PostMapper postMapper;

    public PostService(PostRepository postRepository, PostLookUpService postLookUpService,
                       UserLookupService userLookupService, PostMapper postMapper) {
        this.postRepository = postRepository;
        this.postLookUpService = postLookUpService;
        this.userLookupService = userLookupService;
        this.postMapper = postMapper;
    }

    public List<PostResponse> getPosts() {
        return postRepository.findAll().stream()
                .map(postMapper::toResponse)
                .toList();
    }

    public Post getPostById(Long id) {
        return postLookUpService.getPostById(id);
    }

    public PostResponse getPostResponseById(Long id) {
        return postMapper.toResponse(getPostById(id));
    }

    @Transactional
    public PostResponse insertPost(PostRequest postRequest, UserPrincipal userPrincipal) {
        Post post = postMapper.toEntity(postRequest);
        post.setUser(userLookupService.getById(userPrincipal.getId()));
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
