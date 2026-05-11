package com.example.demo.ApartmentPhoto;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Cloudinary.CloudinaryService;

@ExtendWith(MockitoExtension.class)
public class ApartmentPhotoServiceTest {

    private ApartmentPhotoService apartmentPhotoService;

    @Mock
    private ApartmentPhotoRepository apartmentPhotoRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @BeforeEach
    public void setUp() {
        apartmentPhotoService = new ApartmentPhotoService(apartmentPhotoRepository, cloudinaryService);
    }

    @Test
    @DisplayName("saveImages should return early for null or empty input")
    public void saveImages_WhenImagesNullOrEmpty_ReturnsEarly() throws IOException {
        ApartmentEntity apartment = apartment(10);

        apartmentPhotoService.saveImages(apartment, null, false);
        apartmentPhotoService.saveImages(apartment, List.of(), false);

        verify(apartmentPhotoRepository, never()).findByApartmentId(any(Integer.class));
        verify(apartmentPhotoRepository, never()).save(any(ApartmentPhotoEntity.class));
        verify(apartmentPhotoRepository, never()).delete(any(ApartmentPhotoEntity.class));
        verify(cloudinaryService, never()).upload(any(MultipartFile.class), eq("apartments"));
    }

    @Test
    @DisplayName("saveImages should continue ordering and map upload fields when replace is false")
    public void saveImages_ReplaceFalse_ContinuesOrderAndMapsUploadResult() throws IOException {
        ApartmentEntity apartment = apartment(20);
        ApartmentPhotoEntity existingA = photo(apartment, 1, true, "old-1", "https://old/1.jpg");
        ApartmentPhotoEntity existingB = photo(apartment, 3, false, "old-3", "https://old/3.jpg");
        ApartmentPhotoEntity existingWithoutOrder = photo(apartment, null, false, "old-null", "https://old/null.jpg");

        MultipartFile firstFile = org.mockito.Mockito.mock(MultipartFile.class);
        MultipartFile secondFile = org.mockito.Mockito.mock(MultipartFile.class);

        when(apartmentPhotoRepository.findByApartmentId(20))
                .thenReturn(List.of(existingA, existingB, existingWithoutOrder));
        when(cloudinaryService.upload(firstFile, "apartments"))
                .thenReturn(Map.of("secure_url", "https://img/new-4.jpg", "public_id", "new-4"));
        when(cloudinaryService.upload(secondFile, "apartments"))
                .thenReturn(Map.of("secure_url", "https://img/new-5.jpg", "public_id", "new-5"));

        apartmentPhotoService.saveImages(apartment, List.of(firstFile, secondFile), false);

        ArgumentCaptor<ApartmentPhotoEntity> captor = ArgumentCaptor.forClass(ApartmentPhotoEntity.class);
        verify(apartmentPhotoRepository, times(2)).save(captor.capture());

        List<ApartmentPhotoEntity> saved = captor.getAllValues();
        assertEquals(4, saved.get(0).getPhoto_order());
        assertEquals(false, saved.get(0).getCover());
        assertEquals("https://img/new-4.jpg", saved.get(0).getUrl());
        assertEquals("new-4", saved.get(0).getPublicId());
        assertEquals(apartment, saved.get(0).getApartment());

        assertEquals(5, saved.get(1).getPhoto_order());
        assertEquals(false, saved.get(1).getCover());
        assertEquals("https://img/new-5.jpg", saved.get(1).getUrl());
        assertEquals("new-5", saved.get(1).getPublicId());
        assertEquals(apartment, saved.get(1).getApartment());
    }

    @Test
    @DisplayName("saveImages should delete current photos and restart ordering when replace is true")
    public void saveImages_ReplaceTrue_DeletesExistingAndRestartsOrder() throws IOException {
        ApartmentEntity apartment = apartment(30);
        ApartmentPhotoEntity existing = photo(apartment, 2, false, "old-2", "https://old/2.jpg");
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

        when(apartmentPhotoRepository.findByApartmentId(30))
                .thenReturn(List.of(existing))
                .thenReturn(List.of());
        when(cloudinaryService.upload(file, "apartments"))
                .thenReturn(Map.of("secure_url", "https://img/new-1.jpg", "public_id", "new-1"));

        apartmentPhotoService.saveImages(apartment, List.of(file), true);

        verify(apartmentPhotoRepository).delete(existing);
        verify(apartmentPhotoRepository, times(2)).findByApartmentId(30);

        ArgumentCaptor<ApartmentPhotoEntity> captor = ArgumentCaptor.forClass(ApartmentPhotoEntity.class);
        verify(apartmentPhotoRepository).save(captor.capture());
        ApartmentPhotoEntity saved = captor.getValue();
        assertEquals(1, saved.getPhoto_order());
        assertEquals(true, saved.getCover());
        assertEquals("https://img/new-1.jpg", saved.getUrl());
        assertEquals("new-1", saved.getPublicId());
    }

    @Test
    @DisplayName("saveImages should throw RuntimeException when upload fails")
    public void saveImages_WhenUploadFails_ThrowsRuntimeException() throws IOException {
        ApartmentEntity apartment = apartment(40);
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        IOException ioException = new IOException("upload failed");

        when(apartmentPhotoRepository.findByApartmentId(40)).thenReturn(List.of());
        when(cloudinaryService.upload(file, "apartments")).thenThrow(ioException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> apartmentPhotoService.saveImages(apartment, List.of(file), false));

        assertEquals("Error uploading apartment images", exception.getMessage());
        assertEquals(ioException, exception.getCause());
        verify(apartmentPhotoRepository, never()).save(any(ApartmentPhotoEntity.class));
    }

    @Test
    @DisplayName("findById should return photo when present")
    public void findById_ReturnsPhotoWhenPresent() {
        ApartmentPhotoEntity entity = new ApartmentPhotoEntity();
        entity.setId(50);

        when(apartmentPhotoRepository.findById(50)).thenReturn(Optional.of(entity));

        ApartmentPhotoEntity result = apartmentPhotoService.findById(50);

        assertEquals(50, result.getId());
    }

    @Test
    @DisplayName("findById should throw when photo is missing")
    public void findById_WhenMissing_Throws() {
        when(apartmentPhotoRepository.findById(51)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> apartmentPhotoService.findById(51));

        assertEquals("Apartment photo not found", exception.getMessage());
    }

    @Test
    @DisplayName("deleteByPublicId should delete photo when it exists")
    public void deleteByPublicId_WhenFound_DeletesPhoto() {
        ApartmentPhotoEntity entity = new ApartmentPhotoEntity();
        entity.setId(60);
        entity.setPublicId("public-60");

        when(apartmentPhotoRepository.findByPublicId("public-60")).thenReturn(entity);

        apartmentPhotoService.deleteByPublicId("public-60");

        verify(apartmentPhotoRepository).delete(entity);
    }

    @Test
    @DisplayName("deleteByApartmentId should delete each photo for apartment")
    public void deleteByApartmentId_WhenPhotosExist_DeletesEach() {
        ApartmentEntity apartment = apartment(70);
        ApartmentPhotoEntity first = photo(apartment, 1, true, "p-1", "https://img/1.jpg");
        ApartmentPhotoEntity second = photo(apartment, 2, false, "p-2", "https://img/2.jpg");

        when(apartmentPhotoRepository.findByApartmentId(70)).thenReturn(List.of(first, second));

        apartmentPhotoService.deleteByApartmentId(70);

        verify(apartmentPhotoRepository).delete(first);
        verify(apartmentPhotoRepository).delete(second);
    }

    @Test
    @DisplayName("deleteByApartmentId should not delete when apartment has no photos")
    public void deleteByApartmentId_WhenNoPhotos_DoesNotDelete() {
        when(apartmentPhotoRepository.findByApartmentId(71)).thenReturn(List.of());

        apartmentPhotoService.deleteByApartmentId(71);

        verify(apartmentPhotoRepository, never()).delete(any(ApartmentPhotoEntity.class));
    }

    @Test
    @DisplayName("save should delegate to repository and return saved entity")
    public void save_DelegatesToRepository() {
        ApartmentPhotoEntity photo = new ApartmentPhotoEntity();
        photo.setId(80);
        photo.setUrl("https://img/80.jpg");

        when(apartmentPhotoRepository.save(photo)).thenReturn(photo);

        ApartmentPhotoEntity result = apartmentPhotoService.save(photo);

        assertEquals(photo, result);
        verify(apartmentPhotoRepository).save(photo);
    }

    @Test
    @DisplayName("deleteById should delegate deleteById to repository")
    public void deleteById_DelegatesToRepository() {
        apartmentPhotoService.deleteById(90);

        verify(apartmentPhotoRepository).deleteById(90);
    }

    @Test
    @DisplayName("findPhotosByApartmentId should return photos for the given apartment")
    public void findPhotosByApartmentId_ReturnsPhotos() {
        ApartmentEntity apartment = apartment(100);
        ApartmentPhotoEntity first = photo(apartment, 1, true, "p-1", "https://img/1.jpg");
        ApartmentPhotoEntity second = photo(apartment, 2, false, "p-2", "https://img/2.jpg");

        when(apartmentPhotoRepository.findByApartmentId(100)).thenReturn(List.of(first, second));

        List<ApartmentPhotoEntity> result = apartmentPhotoService.findPhotosByApartmentId(100);

        assertEquals(2, result.size());
        verify(apartmentPhotoRepository).findByApartmentId(100);
    }

    @Test
    @DisplayName("findPhotosByApartmentId should return empty list when apartment has no photos")
    public void findPhotosByApartmentId_WhenNoPhotos_ReturnsEmptyList() {
        when(apartmentPhotoRepository.findByApartmentId(101)).thenReturn(List.of());

        List<ApartmentPhotoEntity> result = apartmentPhotoService.findPhotosByApartmentId(101);

        assertEquals(0, result.size());
        verify(apartmentPhotoRepository).findByApartmentId(101);
    }

    private ApartmentEntity apartment(Integer id) {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(id);
        return apartment;
    }

    private ApartmentPhotoEntity photo(ApartmentEntity apartment, Integer orden, Boolean portada, String publicId, String url) {
        ApartmentPhotoEntity photo = new ApartmentPhotoEntity();
        photo.setApartment(apartment);
        photo.setPhoto_order(orden);
        photo.setCover(portada);
        photo.setPublicId(publicId);
        photo.setUrl(url);
        return photo;
    }
}
