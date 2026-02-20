package com.example.demo.User.DTOs;

import com.example.demo.User.UserEntity;

import java.util.List;

public record UserDTO(Integer id, String email, String role) {

    public static UserDTO fromUserEntity(UserEntity user) {
        return new UserDTO(user.getId(), user.getEmail(), user.getRole().name());
    }

    public static List<UserDTO> fromUserEntityList(List<UserEntity> users) {

        return users.stream()
                .map(UserDTO::fromUserEntity)
                .toList();
    }
}
