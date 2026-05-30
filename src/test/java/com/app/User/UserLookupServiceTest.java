package com.app.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserLookupService Unit Tests")
class UserLookupServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserLookupService userLookupService;

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return user when valid ID is provided")
        void shouldReturnUser_WhenUserExists() {
            User user = new User();
            user.setId(1L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            User result = userLookupService.getById(1L);

            assertSame(user, result);
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when user is not found")
        void shouldThrow_WhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () -> userLookupService.getById(1L));
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when ID is null")
        void shouldThrow_WhenIdIsNull() {
            assertThrows(NoSuchElementException.class, () -> userLookupService.getById(null));
            verify(userRepository).findById(null);
        }
    }
}
