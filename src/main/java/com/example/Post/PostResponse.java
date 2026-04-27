package com.example.Post;


public record PostResponse(
        Integer id,
        String productName,
        String description,
        Integer price,
        String picUrl,
        Integer userId,
        PostStatus status,
        java.time.LocalDateTime createdAt
) {
}
