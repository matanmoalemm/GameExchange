package com.example.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(
                          @Pattern(regexp = "^[a-zA-Z0-9._-]*$", message = "Username can only contain letters, numbers, dots, and underscores")
                          @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
                          String username,

                          @NotBlank(message = "Email is required")
                          @Email(message = "Please provide a valid email address")
                          String email) {
}
