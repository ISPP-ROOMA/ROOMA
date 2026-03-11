package com.example.demo.Cloudinary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.ApartmentPhoto.ApartmentPhotoService;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@RestController
@RequestMapping("/api/images")
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;
    private final ApartmentPhotoService apartmentPhotoService;

    private final UserService userService;
    private final ApartmentService apartmentService;

    public CloudinaryController(CloudinaryService cloudinaryService,
                                ApartmentPhotoService apartmentPhotoService,
                                UserService userService,
                                ApartmentService apartmentService) {
        this.cloudinaryService = cloudinaryService;
        this.apartmentPhotoService = apartmentPhotoService;
        this.userService = userService;
        this.apartmentService = apartmentService;
    }

    @PostMapping("/apartment/{id}")
    public ResponseEntity<?> uploadApartmentImages(@PathVariable Integer id, @RequestParam("files") MultipartFile[] files)
            throws IOException {
        ApartmentEntity apartment = apartmentService.findById(id);

        List<MultipartFile> images = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                images.add(file);
            }
        }

        apartmentPhotoService.saveImages(apartment, images, false);
        return ResponseEntity.ok("Imágenes subidas correctamente");
    }

    @PostMapping("/user/{id}/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(@PathVariable Integer id, @RequestParam("file") MultipartFile file)
            throws IOException {
        Map<?, ?> result = cloudinaryService.upload(file, "users");

        UserEntity user = userService.findById(id);
        user.setProfileImageUrl((String) result.get("secure_url"));
        user.setProfileImagePublicId((String) result.get("public_id"));
        userService.save(user);

        return ResponseEntity.ok(user.getProfileImageUrl());
    }

    @PostMapping("/user/{id}/delete-profile-picture")
    public ResponseEntity<?> deleteProfilePicture(@PathVariable Integer id) throws IOException {
        UserEntity user = userService.findById(id);
        cloudinaryService.deleteByPublicId(user.getProfileImagePublicId());
        user.setProfileImageUrl(null);
        user.setProfileImagePublicId(null);
        userService.save(user);
        return ResponseEntity.ok("Profile picture deleted successfully");
    }

    @PostMapping("/user/me/profile-picture")
    public ResponseEntity<?> uploadMyProfilePicture(@RequestParam("file") MultipartFile file)
            throws IOException {
        UserEntity user = userService.findCurrentUserEntity();

        if (user.getProfileImagePublicId() != null) {
            cloudinaryService.deleteByPublicId(user.getProfileImagePublicId());
        }

        Map<?, ?> result = cloudinaryService.upload(file, "users");
        user.setProfileImageUrl((String) result.get("secure_url"));
        user.setProfileImagePublicId((String) result.get("public_id"));
        userService.save(user);

        return ResponseEntity.ok(user.getProfileImageUrl());
    }

    @PostMapping("/user/me/delete-profile-picture")
    public ResponseEntity<?> deleteMyProfilePicture() throws IOException {
        UserEntity user = userService.findCurrentUserEntity();
        if (user.getProfileImagePublicId() != null) {
            cloudinaryService.deleteByPublicId(user.getProfileImagePublicId());
        }
        user.setProfileImageUrl(null);
        user.setProfileImagePublicId(null);
        userService.save(user);
        return ResponseEntity.ok("Profile picture deleted successfully");
    }

} 