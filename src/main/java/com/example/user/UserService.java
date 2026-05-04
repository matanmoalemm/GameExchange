package com.example.user;

import com.example.Post.PostMapper;
import com.example.Post.PostResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PostMapper postMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper, PostMapper postMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.postMapper = postMapper;
    }



    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream().
                map(userMapper::toResponse).toList(); }


    public User getUserById(Integer id){
        return userRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException(id + " number not found"));
    }


    public UserResponse getUserResponseById(Integer id){
        return userMapper.toResponse(getUserById(id));
    }

    public void deleteUserById(Integer id){
        userRepository.deleteById(id);
    }


    public void insertUser(UserRequest user) {
        userRepository.save(userMapper.toEntity(user));
    }

    public List<PostResponse> getUserPostsById(Integer id) {
        User user  = getUserById(id);
        return userMapper.toPostResponses(user.getPosts());
    }

    @Transactional
    public void updateUserFromDto(Integer id,UserUpdateDTO dto){

        userMapper.updateUserFromDto(dto,getUserById(id));
    }
}
