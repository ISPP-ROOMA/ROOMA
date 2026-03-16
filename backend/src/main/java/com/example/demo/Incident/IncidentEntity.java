package com.example.demo.Incident;

import java.time.LocalDateTime;

import com.example.demo.Apartment.ApartmentEntity;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "incidents")
public class IncidentEntity {
    
    @Id
    @SequenceGenerator(name = "incidents_seq", sequenceName = "incidents_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "incidents_seq")
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column()
    private LocalDateTime resolvedAt;    

    @ManyToOne(optional=false)
    private ApartmentEntity apartment;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private UserEntity tenant;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private UserEntity landlord;


    public IncidentEntity() {
    }

    public IncidentEntity(Integer id, String title, String description, IncidentStatus status, LocalDateTime createdAt, LocalDateTime resolvedAt, ApartmentEntity apartment, UserEntity tenant, UserEntity landlord) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.apartment = apartment;
        this.tenant = tenant;
        this.landlord = landlord;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public ApartmentEntity getApartment() {
        return apartment;
    }

    public UserEntity getTenant() {
        return tenant;
    }

    public UserEntity getLandlord() {
        return landlord;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public void setApartment(ApartmentEntity apartment) {
        this.apartment = apartment;
    }

    public void setTenant(UserEntity tenant) {
        this.tenant = tenant;
    }

    public void setLandlord(UserEntity landlord) {
        this.landlord = landlord;
    }
    
}
