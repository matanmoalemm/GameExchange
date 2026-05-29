package com.app.user;

import org.springframework.stereotype.Service;
import java.util.NoSuchElementException;

@Service
public class UserLookupService {
    private final UserRepository userRepository;

    public UserLookupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(
            () -> new NoSuchElementException("User id: " + id + " was not found")
        );
    }
}
