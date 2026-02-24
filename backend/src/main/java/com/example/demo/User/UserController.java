package com.example.demo.User;

import com.example.demo.User.DTOs.CreateUser;
import com.example.demo.User.DTOs.UpdateUser;

import com.example.demo.User.DTOs.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUserProfile() {
        UserDTO user = UserDTO.fromUserEntity(userService.getUserProfile());
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = UserDTO.fromUserEntityList(userService.findAll());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getUserById(@PathVariable Integer id) {
        UserEntity user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<UserEntity> createUser(@RequestBody CreateUser user) {
        UserEntity newUser = new UserEntity();
        newUser.setEmail(user.email());
        newUser.setPassword(user.password());
        newUser.setRole(user.role());

        UserEntity createdUser = userService.save(newUser);

        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserEntity> updateUser(@PathVariable Integer id, @RequestBody UpdateUser user) {
        UserEntity userToUpdate = new UserEntity();
        userToUpdate.setEmail(user.email());
        userToUpdate.setPassword(user.password());
        userToUpdate.setRole(user.role());

        UserEntity updatedUser = userService.update(id, userToUpdate);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        try {
            userService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}



