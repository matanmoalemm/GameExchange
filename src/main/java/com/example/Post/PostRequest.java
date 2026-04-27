package com.example.Post;

public record PostRequest(
        String productName,
        Integer price,
        String picUrl,
        Integer userId,
        String description
) {}