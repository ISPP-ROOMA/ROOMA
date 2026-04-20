package com.example.demo.ApartmentPhoto.dto;

import java.util.List;

import com.example.demo.ApartmentPhoto.ApartmentPhotoEntity;

public record ApartmentPhotoDTO(
        Integer id,
        String url,
        String publicId,
        Integer orden,
        Boolean portada
) {
    public static ApartmentPhotoDTO fromEntity(ApartmentPhotoEntity entity) {
        return new ApartmentPhotoDTO(
                entity.getId(),
                entity.getUrl(),
                entity.getPublicId(),
                entity.getPhoto_order(),
                entity.getCover()
        );
    }

    public static List<ApartmentPhotoDTO> fromEntityList(List<ApartmentPhotoEntity> entities) {
        return entities.stream().map(ApartmentPhotoDTO::fromEntity).toList();
    }
}
