package com.example.demo.ApartmentPhoto;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Cloudinary.CloudinaryService;


@Service
public class ApartmentPhotoService {
    
    private final ApartmentPhotoRepository apartmentPhotoRepository;
    private final CloudinaryService cloudinaryService;

    public ApartmentPhotoService(ApartmentPhotoRepository apartmentPhotoRepository,
            CloudinaryService cloudinaryService) {
        this.apartmentPhotoRepository = apartmentPhotoRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional
    public ApartmentPhotoEntity save(ApartmentPhotoEntity newPhoto) {
        return apartmentPhotoRepository.save(newPhoto);
    }

    @Transactional
    public void saveImages(ApartmentEntity apartment, List<MultipartFile> images, boolean replace) {
        if (images == null || images.isEmpty()) return;

        if (replace) {
            deleteByApartmentId(apartment.getId());
        }

        int nextOrder = apartmentPhotoRepository.findByApartmentId(apartment.getId()).stream()
            .map(ApartmentPhotoEntity::getOrden)
            .filter(o -> o != null)
            .max(Comparator.naturalOrder())
            .orElse(0) + 1;

        for (MultipartFile file : images) {
            try {
                var result = cloudinaryService.upload(file, "apartments");
                ApartmentPhotoEntity img = new ApartmentPhotoEntity();
                img.setApartment(apartment);
                img.setUrl((String) result.get("secure_url"));
                img.setPublicId((String) result.get("public_id"));
                img.setOrden(nextOrder);
                img.setPortada(nextOrder == 1); // primera como portada
                apartmentPhotoRepository.save(img);
                nextOrder++;
            } catch (IOException e) {
                throw new RuntimeException("Error uploading apartment images", e);
            }
        }
    }

    @Transactional(readOnly = true)
    public ApartmentPhotoEntity findById(Integer id) {
        return apartmentPhotoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Apartment photo not found"));
    }

    @Transactional
    public void deleteById(Integer id) {
        apartmentPhotoRepository.deleteById(id);
    }

    @Transactional()
    public void deleteByPublicId(String publicId) {
        ApartmentPhotoEntity photo = apartmentPhotoRepository.findByPublicId(publicId);
        apartmentPhotoRepository.delete(photo);
    }

    @Transactional(readOnly = true)
    public List<ApartmentPhotoEntity> findPhotosByApartmentId(Integer apartmentId) {
        return apartmentPhotoRepository.findByApartmentId(apartmentId);
    }

    @Transactional()
    public void deleteByApartmentId(Integer apartmentId) {
        List<ApartmentPhotoEntity> photos = apartmentPhotoRepository.findByApartmentId(apartmentId);
        for (ApartmentPhotoEntity photo : photos) {
            apartmentPhotoRepository.delete(photo);
        }
    }
}
