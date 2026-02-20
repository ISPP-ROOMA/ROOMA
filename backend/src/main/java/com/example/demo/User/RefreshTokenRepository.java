package com.example.demo.User;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Integer> {

    long deleteByUserAndDeviceId(UserEntity user, String deviceId);

    Optional<RefreshTokenEntity> findByUserAndDeviceId(UserEntity user, String deviceId);
}
