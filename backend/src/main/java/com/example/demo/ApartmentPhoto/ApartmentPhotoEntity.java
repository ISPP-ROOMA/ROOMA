package com.example.demo.ApartmentPhoto;

import com.example.demo.Apartment.ApartmentEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String publicId;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private Boolean portada;

    @ManyToOne(optional = false)
    private ApartmentEntity apartment;

    public ApartmentPhotoEntity() {
    }

    public ApartmentPhotoEntity(Integer id, String url, String publicId, Integer orden, Boolean portada, ApartmentEntity apartment) {
        this.id = id;
        this.url = url;
        this.publicId = publicId;
        this.orden = orden;
        this.portada = portada;
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

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Boolean getPortada() {
        return portada;
    }

    public void setPortada(Boolean portada) {
        this.portada = portada;
    }

    public ApartmentEntity getApartment() {
        return apartment;
    }

    public void setApartment(ApartmentEntity apartment) {
        this.apartment = apartment;
    }
}
