package com.example.demo.ApartmentPhoto;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ApartmentPhotoService {
    
    private final ApartmentPhotoRepository apartmentPhotoRepository;

    public ApartmentPhotoService(ApartmentPhotoRepository apartmentPhotoRepository) {
        this.apartmentPhotoRepository = apartmentPhotoRepository;
    }

    @Transactional
    public ApartmentPhotoEntity save(ApartmentPhotoEntity newPhoto) {
        return apartmentPhotoRepository.save(newPhoto);
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
