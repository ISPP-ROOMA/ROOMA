package com.example.demo.User;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Jwt.UserDetailsImpl;
import com.example.demo.User.DTOs.UpdateProfileRequest;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity save(UserEntity newUser) {
        return userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public UserEntity findById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }

    @Transactional
    public UserEntity update(Integer id, UserEntity user) {
        UserEntity existingUser = findById(id);

        existingUser.setEmail(user.getEmail());
        existingUser.setRole(user.getRole());

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(existingUser);
    }

    @Transactional
    public void deleteById(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

    public UserEntity getUserProfile() {
        return findByEmail(findCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    }

    public String findCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            throw new ResourceNotFoundException("Auth not found");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails.getUsername();
        }

        throw new ResourceNotFoundException("User not authenticated");
    }

    public UserEntity findCurrentUserEntity() {
        String email = findCurrentUser();
        return findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public UserEntity updateCurrentUserProfile(UpdateProfileRequest request) {
        UserEntity currentUser = findCurrentUserEntity();

        String normalizedEmail = normalize(request.email());
        if (normalizedEmail != null && !normalizedEmail.equals(currentUser.getEmail())) {
            Optional<UserEntity> existing = userRepository.findByEmail(normalizedEmail);
            if (existing.isPresent() && !existing.get().getId().equals(currentUser.getId())) {
                throw new ConflictException("Email already in use");
            }
            currentUser.setEmail(normalizedEmail);
        }

        if (request.password() != null && !request.password().isBlank()) {
            currentUser.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.name() != null) {
            currentUser.setName(normalize(request.name()));
        }

        if (request.surname() != null) {
            currentUser.setSurname(normalize(request.surname()));
        }

        if (request.birthDate() != null) {
            currentUser.setBirthDate(parseBirthDate(request.birthDate()));
        }

        if (request.phone() != null) {
            currentUser.setPhone(normalize(request.phone()));
        }

        if (request.gender() != null) {
            currentUser.setGender(normalize(request.gender()));
        }

        if (request.smoker() != null) {
            currentUser.setSmoker(request.smoker());
        }

        if (request.profilePic() != null) {
            currentUser.setProfileImageUrl(normalize(request.profilePic()));
        }

        if (request.hobbies() != null) {
            currentUser.setHobbies(normalize(request.hobbies()));
        }

        if (request.schedule() != null) {
            currentUser.setSchedule(normalize(request.schedule()));
        }

        if (request.profession() != null) {
            currentUser.setProfession(normalize(request.profession()));
        }

        return userRepository.save(currentUser);
    }

    @Transactional
    public void deleteCurrentUserProfile() {
        UserEntity currentUser = findCurrentUserEntity();
        try {
            userRepository.delete(currentUser);
            userRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("User has related data and cannot be deleted");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDate parseBirthDate(String value) {
        String trimmed = normalize(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ex) {
            try {
                return OffsetDateTime.parse(trimmed).toLocalDate();
            } catch (DateTimeParseException inner) {
                throw new BadRequestException("Invalid birthDate format");
            }
        }
    }


}



