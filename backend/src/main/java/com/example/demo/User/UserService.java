package com.example.demo.User;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Jwt.UserDetailsImpl;

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

}



