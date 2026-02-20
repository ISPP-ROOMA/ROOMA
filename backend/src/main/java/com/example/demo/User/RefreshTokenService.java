package com.example.demo.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void save(RefreshTokenEntity refreshTokenEntity) {
        refreshTokenRepository.save(refreshTokenEntity);
    }

    public RefreshTokenEntity findByUserAndDeviceId(UserEntity user, String deviceId) {
        return refreshTokenRepository.findByUserAndDeviceId(user, deviceId)
                .orElse(new RefreshTokenEntity());
    }

    @Transactional
    public long deleteByUserAndDeviceId(UserEntity user, String deviceId) {
        long result = refreshTokenRepository.deleteByUserAndDeviceId(user, deviceId);
        refreshTokenRepository.flush();
        return result;
    }

    @Transactional
    public void delete(RefreshTokenEntity storedToken) {
        refreshTokenRepository.deleteById(storedToken.getId());
    }
}
