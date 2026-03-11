package com.example.demo.Favorite;

import java.time.LocalDateTime;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.User.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "favorites",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorites_user_apartment", columnNames = { "user_id", "apartment_id" })
)
public class FavoriteEntity {

    @Id
    @SequenceGenerator(name = "favorites_seq", sequenceName = "favorites_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "favorites_seq")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private ApartmentEntity apartment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FavoriteEntity() {
    }

    public FavoriteEntity(UserEntity user, ApartmentEntity apartment) {
        this.user = user;
        this.apartment = apartment;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public ApartmentEntity getApartment() {
        return apartment;
    }

    public void setApartment(ApartmentEntity apartment) {
        this.apartment = apartment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
