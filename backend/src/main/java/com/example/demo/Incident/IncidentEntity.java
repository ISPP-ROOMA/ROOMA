package com.example.demo.Incident;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.User.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IncidentCategory category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IncidentZone zone;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IncidentUrgency urgency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime resolvedAt;

    @Column
    private LocalDateTime closedAt;

    @Column(length = 1000)
    private String rejectionReason;

    @ElementCollection
    @CollectionTable(name = "incident_attachments", joinColumns = @JoinColumn(name = "incident_id"))
    @Column(name = "photo_url", nullable = false)
    private List<String> photos = new ArrayList<>();

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

    public IncidentEntity(Integer id,
                          String title,
                          String description,
                          IncidentCategory category,
                          IncidentZone zone,
                          IncidentUrgency urgency,
                          IncidentStatus status,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt,
                          LocalDateTime resolvedAt,
                          LocalDateTime closedAt,
                          String rejectionReason,
                          List<String> photos,
                          ApartmentEntity apartment,
                          UserEntity tenant,
                          UserEntity landlord) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.zone = zone;
        this.urgency = urgency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
        this.closedAt = closedAt;
        this.rejectionReason = rejectionReason;
        this.photos = photos == null ? new ArrayList<>() : photos;
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

    public IncidentCategory getCategory() {
        return category;
    }

    public IncidentZone getZone() {
        return zone;
    }

    public IncidentUrgency getUrgency() {
        return urgency;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public List<String> getPhotos() {
        return photos;
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

    public void setCategory(IncidentCategory category) {
        this.category = category;
    }

    public void setZone(IncidentZone zone) {
        this.zone = zone;
    }

    public void setUrgency(IncidentUrgency urgency) {
        this.urgency = urgency;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos == null ? new ArrayList<>() : photos;
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
