package com.example.demo.Favorite;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Integer> {

    Optional<FavoriteEntity> findByUserIdAndApartmentId(Integer userId, Integer apartmentId);

    void deleteByUserIdAndApartmentId(Integer userId, Integer apartmentId);

    List<FavoriteEntity> findByUserIdOrderByCreatedAtDesc(Integer userId);

    @Query("SELECT f.apartment.id FROM FavoriteEntity f WHERE f.user.id = :userId AND f.apartment.id IN :apartmentIds")
    List<Integer> findFavoriteApartmentIdsByUserIdAndApartmentIds(
            @Param("userId") Integer userId,
            @Param("apartmentIds") List<Integer> apartmentIds
    );
}
