package com.app.user;


import com.app.Post.PostResponse;
import com.app.Security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

// --- Private Endpoints (Actions on the authenticated user) ---

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        // Retrieve the profile of the currently logged-in user using the secure ID from the Principal
        return userService.getUserResponseById(principal.getId());
    }

    @GetMapping("/me/posts")
    @ResponseStatus(HttpStatus.OK)
    public List<PostResponse> getMyPosts(@AuthenticationPrincipal UserPrincipal principal) {
        // Fetch posts belonging specifically to the authenticated user
        return userService.getUserPostsById(principal.getId());
    }

    @PatchMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMyProfile(@AuthenticationPrincipal UserPrincipal principal,
                                @Valid @RequestBody UserUpdateDTO dto) {
        // Only the authenticated user can update their own profile data
        userService.updateUserFromDto(principal.getId(), dto);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(@AuthenticationPrincipal UserPrincipal principal) {
        // Securely delete the account of the authenticated user
        userService.deleteUserById(principal.getId());
    }

    // --- Public/Management Endpoints (Interacting with other users) ---

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UserResponse> getAllUsers() {
        // Retrieve a list of all users - usually for admin purposes or user discovery
        return userService.getUsers();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getUserPublicProfile(@PathVariable Long id) {
        // View a specific user's public profile (e.g., when clicking on a seller's name)
        return userService.getUserResponseById(id);
    }

    @GetMapping("/{id}/posts")
    @ResponseStatus(HttpStatus.OK)
    public List<PostResponse> getPostsByUserId(@PathVariable Long id) {
        // View all game posts listed by a specific user
        return userService.getUserPostsById(id);
    }


}
