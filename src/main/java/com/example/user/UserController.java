package com.example.user;


import com.example.Post.Post;
import jakarta.validation.constraints.Email;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getUsers(){

        return userService.getUsers();
    }

    @GetMapping("{id}")
    public User getUserById(
            @PathVariable Integer id){
        return userService.getUserById(id);
    }

    @GetMapping("{id}/posts")
    public List<Post> getUserPostsById(
            @PathVariable Integer id){
        return userService.getUserPostsById(id);
    }
    @PostMapping
    public void addNewUser(
            @RequestBody User user
    ){
        userService.insertUser(user);
    }
    @DeleteMapping("{id}")
    public void deleteUser(
            @PathVariable Integer id){
        userService.deleteUserById(id);
    }

    @PutMapping("{id}/username")
    public void updateUsername(
            @PathVariable Integer id, @RequestParam String username){
        userService.updateUsernameById(id,username);
    }
    @PutMapping("{id}/email")
    public void updateUserEmail(
            @PathVariable Integer id, @RequestParam @Email String email){
        userService.updateUserEmailById(id,email);
    }



}
