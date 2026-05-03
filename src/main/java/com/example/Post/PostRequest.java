package com.example.Post;

import jakarta.validation.constraints.*;

public record PostRequest(
        @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
        String productName,

        @NotNull(message = "Price is required")
        @PositiveOrZero(message = "Price must be 0 or greater")
        Integer price,

        @NotBlank(message = "Image URL is required")
        @Pattern(regexp = "^(https?|ftp)://.*$", message = "PicUrl must be a valid URL")
        String picUrl,

        @NotNull(message = "User ID is required")
        Integer userId,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description
) {}