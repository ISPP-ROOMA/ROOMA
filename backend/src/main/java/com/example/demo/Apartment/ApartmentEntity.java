package com.example.demo.Apartment;

import com.example.demo.User.UserEntity;

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
@Table(name = "apartments")
public class ApartmentEntity {

    @Id
    @SequenceGenerator(name = "apartments_seq", sequenceName = "apartments_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartments_seq")
    private Integer id;

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

    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    private UserEntity user;

    @Column
    private String imageUrl;

    public ApartmentEntity() {
    }

    public ApartmentEntity(String title, String description, Double price, String bills, String ubication,
            ApartmentState state,
            UserEntity user, String imageUrl) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.bills = bills;
        this.ubication = ubication;
        this.state = state;
        this.user = user;
        this.imageUrl = imageUrl;
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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}