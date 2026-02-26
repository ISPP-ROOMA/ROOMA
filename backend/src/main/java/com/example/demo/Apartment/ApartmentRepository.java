package com.example.demo.Apartment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApartmentRepository extends JpaRepository<ApartmentEntity, Integer> {

    @Query("SELECT a FROM ApartmentEntity a WHERE " +
            "(:ubication IS NULL OR a.ubication LIKE %:ubication%) AND " +
            "(:minPrice IS NULL OR a.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR a.price <= :maxPrice) AND " +
            "(:state IS NULL OR a.state = :state)")
    List<ApartmentEntity> search(@Param("ubication") String ubication,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("state") ApartmentState state);

    public List<ApartmentEntity> findAllByUserId(Integer userId);
}
