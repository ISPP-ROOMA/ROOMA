package com.example.demo.Apartment;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.ApartmentPhoto.ApartmentPhotoEntity;
import com.example.demo.User.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "apartments")
public class ApartmentEntity {

    @Id
    @SequenceGenerator(name = "apartments_seq", sequenceName = "apartments_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartments_seq")
    private Integer id;

    @JsonIgnore
    @OneToMany(mappedBy = "apartment", fetch = FetchType.EAGER)
    private List<ApartmentPhotoEntity> photos;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column()
    private String bills;

    @Column(nullable = false)
    private String ubication;

    @Enumerated(EnumType.STRING)
    private ApartmentState state;

    @Column(nullable = false)
    private Integer maxTenants;
    @Column
    private LocalDateTime activationDate;

    @Column(length = 1000)
    private String idealTenantProfile;

    @JsonIgnore
    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    private UserEntity user;

    public ApartmentEntity() {
    }

    public ApartmentEntity(String title, String description, Double price, String bills, String ubication,
            ApartmentState state, Integer maxTenants,
            UserEntity user) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.bills = bills;
        this.ubication = ubication;
        this.state = state;
        this.maxTenants = maxTenants;
        this.user = user;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getBills() {
        return bills;
    }

    public void setBills(String bills) {
        this.bills = bills;
    }

    public String getUbication() {
        return ubication;
    }

    public void setUbication(String ubication) {
        this.ubication = ubication;
    }

    public ApartmentState getState() {
        return state;
    }

    public void setState(ApartmentState state) {
        this.state = state;
    }

    public Integer getMaxTenants() {
        return maxTenants;
    }

    public void setMaxTenants(Integer maxTenants) {
        this.maxTenants = maxTenants;
    }
    
    public LocalDateTime getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(LocalDateTime activationDate) {
        this.activationDate = activationDate;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getIdealTenantProfile() {
        return idealTenantProfile;
    }

    public void setIdealTenantProfile(String idealTenantProfile) {
        this.idealTenantProfile = idealTenantProfile;
    }

    public List<ApartmentPhotoEntity> getPhotos() {
        return photos;
    }

    public void setPhotos(List<ApartmentPhotoEntity> photos) {
        this.photos = photos;
    }

    public String getCoverImageUrl() {
        if (photos == null || photos.isEmpty()) {
            return null;
        }

        return photos.stream()
                .filter(photo -> photo.getPhoto_order() != null && photo.getPhoto_order().equals(1))
                .map(ApartmentPhotoEntity::getUrl)
                .findFirst()
                .orElseGet(() -> photos.stream()
                        .map(ApartmentPhotoEntity::getUrl)
                        .findFirst()
                        .orElse(null));
    }
}
