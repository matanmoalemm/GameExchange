package com.example.Post;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record PostUpdateDto(
        @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
        String productName,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @PositiveOrZero(message = "Price must be 0 or greater")
        Integer price,

        @Pattern(regexp = "^(https?|ftp)://.*$", message = "PicUrl must be a valid URL starting with http or https")
        String picUrl,

        @Pattern(regexp = "ACTIVE|PENDING|SOLD|DELETED",
                message = "Invalid status. Must be ACTIVE, PENDING, SOLD, or DELETED")
        String status
) {
}
