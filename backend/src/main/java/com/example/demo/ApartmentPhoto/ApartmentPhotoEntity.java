package com.example.demo.ApartmentPhoto;

import com.example.demo.Apartment.ApartmentEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "apartment_photos")
public class ApartmentPhotoEntity {
    
    @Id
    @SequenceGenerator(name = "apartment_photos_seq",
            sequenceName = "apartment_photos_seq",
            initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartment_photos_seq")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    @JsonIgnore
    private ApartmentEntity apartment;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String publicId;

    @Column(nullable = false)
    private Integer photo_order;

    @Column(nullable = false)
    private Boolean cover;

    public ApartmentPhotoEntity() {
    }

    public ApartmentPhotoEntity(Integer id, String url, String publicId, Integer order, Boolean cover, ApartmentEntity apartment) {
        this.id = id;
        this.url = url;
        this.publicId = publicId;
        this.photo_order = order;
        this.cover = cover;
        this.apartment = apartment;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Integer getPhoto_order() {
        return photo_order;
    }

    public void setPhoto_order(Integer photo_order) {
        this.photo_order = photo_order;
    }

    public Boolean getCover() {
        return cover;
    }

    public void setCover(Boolean cover) {
        this.cover = cover;
    }

    public ApartmentEntity getApartment() {
        return apartment;
    }

    public void setApartment(ApartmentEntity apartment) {
        this.apartment = apartment;
    }
}
