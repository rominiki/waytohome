package com.rominiki.waytohome.controller;

import com.rominiki.waytohome.dto.RegisterRequest;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    @PostMapping("/register")
    public ResponseEntity<User> register(
            @Valid @RequestBody RegisterRequest request) {
        User saved = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
