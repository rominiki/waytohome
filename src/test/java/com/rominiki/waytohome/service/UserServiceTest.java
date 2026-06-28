package com.rominiki.waytohome.service;

import com.rominiki.waytohome.dto.RegisterRequest;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.exception.DuplicateEmailException;
import com.rominiki.waytohome.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    void register_savesUser_withHashedPassword() {
        var request = new RegisterRequest(
                "test@example.com", "plainpassword", "Test User", Role.STUDENT);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainpassword")).thenReturn("$2a$HASH");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User result = userService.register(request);

        assertThat(result.getPassword()).isEqualTo("$2a$HASH");
        assertThat(result.getPassword()).isNotEqualTo("plainpassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsDuplicateEmailException_whenEmailExists() {
        var request = new RegisterRequest(
                "taken@example.com", "password123", "User", Role.STUDENT);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any());
    }
}