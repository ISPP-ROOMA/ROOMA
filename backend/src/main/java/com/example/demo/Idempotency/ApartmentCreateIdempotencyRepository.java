package com.example.demo.Idempotency;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentCreateIdempotencyRepository extends JpaRepository<ApartmentCreateIdempotencyEntity, Long> {
    Optional<ApartmentCreateIdempotencyEntity> findByUserIdAndEndpointAndIdempotencyKey(
            Integer userId,
            String endpoint,
            String idempotencyKey);
}
