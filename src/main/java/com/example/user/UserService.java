package com.example.user;

import com.example.Post.Post;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUsers() { return userRepository.findAll(); }


    public User getUserById(Integer id){
        return userRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException(id + " number not found"));
    }

    public void deleteUserById(Integer id){
        userRepository.deleteById(id);
    }


    public void insertUser(User user) {
        userRepository.save(user);
    }

    public List<Post> getUserPostsById(Integer id) {
        User user  = getUserById(id);
        return user.getPosts();
    }

    @Transactional
    public void updateUsernameById(Integer id, String username){
        User user = getUserById(id);
        user.setUsername(username);

    }

    @Transactional
    public void updateUserEmailById(Integer id, @Email String email) {
        User user = getUserById(id);
        user.setEmail(email);
    }
}
