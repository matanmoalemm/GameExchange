package com.app.User;

import com.app.Post.PostResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserLookupService userLookupService;

    public UserService(UserRepository userRepository, UserMapper userMapper, UserLookupService userLookupService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userLookupService = userLookupService;
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse).toList();
    }

    public User getUserById(Long id) {
        return userLookupService.getById(id);
    }

    public UserResponse getUserResponseById(Long id) {
        return userMapper.toResponse(getUserById(id));
    }

    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }

    public void insertUser(UserRequest user) {
        userRepository.save(userMapper.toEntity(user));
    }

    public List<PostResponse> getUserPostsById(Long id) {
        User user = getUserById(id);
        return userMapper.toPostResponses(user.getPosts());
    }

    @Transactional
    public void updateUserFromDto(Long id, UserUpdateDTO dto) {
        userMapper.updateUserFromDto(dto, getUserById(id));
    }
}
