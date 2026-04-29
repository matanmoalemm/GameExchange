package com.example.user;

import com.example.Post.PostResponse;

import java.util.List;

public record UserResponse(Integer id,
                           String username,
                           List<PostResponse> postResponses
) {
}
