package com.example.demo.User.DTOs;

import java.util.List;

import com.example.demo.User.UserEntity;

public record UserDTO(Integer id, String email, String role, String hobbies, String schedule, String profession) {

    public static UserDTO fromUserEntity(UserEntity user) {
        return new UserDTO(user.getId(), user.getEmail(), user.getRole().name(), user.getHobbies(), user.getSchedule(),
                user.getProfession());
    }

    public static List<UserDTO> fromUserEntityList(List<UserEntity> users) {

        return users.stream()
                .map(UserDTO::fromUserEntity)
                .toList();
    }
}
