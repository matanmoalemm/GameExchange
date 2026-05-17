package com.app.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String text,
        Long postId,
        Long authorId,
        LocalDateTime createdAt
) {}
