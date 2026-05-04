package com.example.user;


import com.example.Post.PostResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.FOUND)
    public List<UserResponse> getUsers(){
        return userService.getUsers();
    }

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.FOUND)
    public UserResponse getUserById(
            @PathVariable Integer id){
        return userService.getUserResponseById(id);
    }

    @GetMapping("{id}/posts")
    @ResponseStatus(HttpStatus.FOUND)
    public List<PostResponse> getUserPostsById(
            @PathVariable Integer id){
        return userService.getUserPostsById(id);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addNewUser(
            @Valid @RequestBody UserRequest user
    ){
        userService.insertUser(user);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @PathVariable Integer id){
        userService.deleteUserById(id);

    }

    @PatchMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUserFromDto(
            @PathVariable Integer id, @Valid @RequestBody UserUpdateDTO dto){
        userService.updateUserFromDto(id,dto);
    }



}
