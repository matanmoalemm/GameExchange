package com.app.user;

import com.app.Post.PostMapper;
import com.app.Post.PostResponse;
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


    public User getUserById(Long id){
        return userRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException(id + " number not found"));
    }


    public UserResponse getUserResponseById(Long id){
        return userMapper.toResponse(getUserById(id));
    }

    public void deleteUserById(Long id){
        userRepository.deleteById(id);
    }


    public void insertUser(UserRequest user) {
        userRepository.save(userMapper.toEntity(user));
    }

    public List<PostResponse> getUserPostsById(Long id) {
        User user  = getUserById(id);
        return userMapper.toPostResponses(user.getPosts());
    }

    @Transactional
    public void updateUserFromDto(Long id,UserUpdateDTO dto){

        userMapper.updateUserFromDto(dto,getUserById(id));
    }

}