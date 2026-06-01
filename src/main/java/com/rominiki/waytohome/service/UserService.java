package com.rominiki.waytohome.service;

import com.rominiki.waytohome.dto.RegisterRequest;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.exception.DuplicateEmailException;
import com.rominiki.waytohome.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password())) // hash!
                .fullName(request.fullName())
                .role(request.role())
                .build();

        return userRepository.save(user);
    }
}
