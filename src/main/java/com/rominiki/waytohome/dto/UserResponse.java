package com.rominiki.waytohome.dto;

import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.enums.Role;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole());
    }
}