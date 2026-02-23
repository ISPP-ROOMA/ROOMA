package com.example.demo.Apartment;


import com.example.demo.User.UserEntity;

import jakarta.persistence.*;

@Entity
@Table(name = "apartments")
public class ApartmentEntity {

    @Id
    @SequenceGenerator(name = "apartments_seq",
            sequenceName = "apartments_seq",
            initialValue = 100)
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

    @Column(nullable = false)
    private String state;

    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    private UserEntity user;

    public ApartmentEntity() {
    }

    public ApartmentEntity(String title, String description, Double price, String bills, String ubication, String state,
            UserEntity user) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.bills = bills;
        this.ubication = ubication;
        this.state = state;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    
    
}