package com.example.demo.User.DTOs;

import java.util.List;

import com.example.demo.User.UserEntity;

public record UserDTO(
    Integer id,
    String email,
    String role,
    String name,
    String gender,
    Boolean smoker,
    String profileImageUrl,
    String hobbies,
    String schedule,
    String profession
) {

    public static UserDTO fromUserEntity(UserEntity user) {
    return new UserDTO(
        user.getId(),
        user.getEmail(),
        user.getRole().name(),
        user.getName(),
        user.getGender(),
        user.getSmoker(),
        user.getProfileImageUrl(),
        user.getHobbies(),
        user.getSchedule(),
        user.getProfession()
    );
    }

    public static List<UserDTO> fromUserEntityList(List<UserEntity> users) {

        return users.stream()
                .map(UserDTO::fromUserEntity)
                .toList();
    }
}
