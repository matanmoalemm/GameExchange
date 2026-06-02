package com.app.Post;

public record PostResponse(
        Long id,
        String productName,
        String description,
        Integer price,
        String picUrl,
        Long userId,
        String status,
        java.time.LocalDateTime createdAt,
        Integer soldPrice
) {
}
