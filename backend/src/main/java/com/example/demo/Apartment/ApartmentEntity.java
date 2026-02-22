package com.example.demo.Apartment;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
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
    @Enumerated(EnumType.STRING)
    private ApartmentState state;

    public ApartmentEntity() {
    }

    public ApartmentEntity(Integer id, String title, String description, Double price, String bills, String ubication, ApartmentState state) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.bills = bills;
        this.ubication = ubication;
        this.state = state;
    }
}