package com.example.demo.User.DTOs;

import com.example.demo.User.UserEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserProfileDTO(
        Integer id,
        String name,
        String surname,
        String email,
        String role,
        LocalDate birthDate,
        String phone,
        String profilePic,
        String gender,
        Boolean smoker,
        LocalDateTime createdAt,
        String hobbies,
        String schedule,
        String profession
) {
    public static UserProfileDTO fromUserEntity(UserEntity user) {
        return new UserProfileDTO(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getRole().name(),
                user.getBirthDate(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.getGender(),
                user.getSmoker(),
                user.getCreatedAt(),
                user.getHobbies(),
                user.getSchedule(),
                user.getProfession()
        );
    }
}
