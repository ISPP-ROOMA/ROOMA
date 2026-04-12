package com.example.demo.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
    }

    @Test
    @DisplayName("save should delegate to repository")
    void save_ShouldPersistEntity() {
        RefreshTokenEntity entity = new RefreshTokenEntity();

        when(refreshTokenRepository.save(entity)).thenReturn(entity);

        refreshTokenService.save(entity);

        verify(refreshTokenRepository).save(entity);
    }

    @Test
    @DisplayName("findByUserAndDeviceId should return stored token when present")
    void findByUserAndDeviceId_ReturnsExistingToken() {
        UserEntity user = new UserEntity();
        user.setId(1);
        String deviceId = "device-1";

        RefreshTokenEntity stored = new RefreshTokenEntity();
        stored.setId(10);
        stored.setUser(user);
        stored.setDeviceId(deviceId);

        when(refreshTokenRepository.findByUserAndDeviceId(user, deviceId))
                .thenReturn(Optional.of(stored));

        RefreshTokenEntity result = refreshTokenService.findByUserAndDeviceId(user, deviceId);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(deviceId, result.getDeviceId());
    }

    @Test
    @DisplayName("findByUserAndDeviceId should return new entity when no token exists")
    void findByUserAndDeviceId_ReturnsNewEntityWhenNotFound() {
        UserEntity user = new UserEntity();
        user.setId(2);
        String deviceId = "device-2";

        when(refreshTokenRepository.findByUserAndDeviceId(user, deviceId))
                .thenReturn(Optional.empty());

        RefreshTokenEntity result = refreshTokenService.findByUserAndDeviceId(user, deviceId);

        assertNotNull(result);
        assertNull(result.getId());
        assertNull(result.getToken());
        verify(refreshTokenRepository).findByUserAndDeviceId(user, deviceId);
    }

    @Test
    @DisplayName("deleteByUser should delete tokens for user and flush")
    void deleteByUser_DeletesAndFlushes() {
        UserEntity user = new UserEntity();
        user.setId(3);

        when(refreshTokenRepository.deleteByUser(user)).thenReturn(3L);

        long deleted = refreshTokenService.deleteByUser(user);

        assertEquals(3L, deleted);
        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).flush();
    }

    @Test
    @DisplayName("deleteByUserAndDeviceId should delete token for user and device and flush")
    void deleteByUserAndDeviceId_DeletesAndFlushes() {
        UserEntity user = new UserEntity();
        user.setId(4);
        String deviceId = "device-4";

        when(refreshTokenRepository.deleteByUserAndDeviceId(user, deviceId)).thenReturn(1L);

        long deleted = refreshTokenService.deleteByUserAndDeviceId(user, deviceId);

        assertEquals(1L, deleted);
        verify(refreshTokenRepository).deleteByUserAndDeviceId(user, deviceId);
        verify(refreshTokenRepository).flush();
    }

    @Test
    @DisplayName("delete should delete token by id")
    void delete_DeletesById() {
        RefreshTokenEntity stored = new RefreshTokenEntity();
        stored.setId(50);

        refreshTokenService.delete(stored);

        verify(refreshTokenRepository).deleteById(50);
        verify(refreshTokenRepository).deleteById(any());
    }
}

