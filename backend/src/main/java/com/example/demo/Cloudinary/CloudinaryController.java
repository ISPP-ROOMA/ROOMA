package com.example.demo.Cloudinary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentRepository;
import com.example.demo.ApartmentPhoto.ApartmentPhotoEntity;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserRepository;

@RestController
@RequestMapping("/api/images")
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;

    public CloudinaryController(CloudinaryService cloudinaryService, UserRepository userRepository, ApartmentRepository apartmentRepository) {
        this.cloudinaryService = cloudinaryService;
        this.userRepository = userRepository;
        this.apartmentRepository = apartmentRepository;
    }

    @PostMapping("/apartment/{id}")
    public ResponseEntity<?> uploadApartmentImages(@PathVariable Integer id, @RequestParam("files") MultipartFile[] files)
            throws IOException {
        ApartmentEntity apartment = apartmentRepository.findById(id).get();

        for (MultipartFile file : files) {
            Map result = cloudinaryService.upload(file, "apartments");

            ApartmentPhotoEntity img = new ApartmentPhotoEntity();
            img.setUrl((String) result.get("secure_url"));
            img.setPublicId((String) result.get("public_id"));
            img.setApartment(apartment);
        }

        apartmentRepository.save(apartment);
        return ResponseEntity.ok("Imágenes subidas correctamente");
    }

    @PostMapping("/user/{id}/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(@PathVariable Integer id, @RequestParam("file") MultipartFile file)
            throws IOException {
        Map result = cloudinaryService.upload(file, "users");

        UserEntity user = userRepository.findById(id).get();
        user.setProfileImageUrl((String) result.get("secure_url"));
        user.setProfileImagePublicId((String) result.get("public_id"));
        userRepository.save(user);

        return ResponseEntity.ok(user.getProfileImageUrl());
    }

    @PostMapping("/user/{id}/delete-profile-picture")
    public ResponseEntity<?> deleteProfilePicture(@PathVariable Integer id) throws IOException {
        UserEntity user = userRepository.findById(id).get();
        cloudinaryService.deleteByPublicId(user.getProfileImagePublicId());
        user.setProfileImageUrl(null);
        user.setProfileImagePublicId(null);
        userRepository.save(user);
        return ResponseEntity.ok("Profile picture deleted successfully");
    }

} 