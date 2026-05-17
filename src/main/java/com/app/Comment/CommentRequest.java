package com.app.Comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotNull(message = "Post ID is required")
        Long postId,

        @NotBlank(message = "Comment text is required")
        @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
        String text
) {}
