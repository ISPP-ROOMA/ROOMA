package com.example.demo.ApartmentPhoto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface ApartmentPhotoRepository extends JpaRepository<ApartmentPhotoEntity, Integer> {
    
    @Query("SELECT p FROM ApartmentPhotoEntity p WHERE p.apartment.id = :apartmentId")
    List<ApartmentPhotoEntity> findByApartmentId(Integer apartmentId);

    @Query("SELECT p FROM ApartmentPhotoEntity p WHERE p.publicId = :publicId")
    ApartmentPhotoEntity findByPublicId(String publicId);
}
