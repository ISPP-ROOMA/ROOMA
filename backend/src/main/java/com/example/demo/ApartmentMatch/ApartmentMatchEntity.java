package com.example.demo.ApartmentMatch;

import java.time.LocalDateTime;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.User.UserEntity;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "apartment_matches")
public class ApartmentMatchEntity {

    @Id
    @SequenceGenerator(name = "apartment_matches_seq",
            sequenceName = "apartment_matches_seq",
            initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartment_matches_seq")
    private Integer id;

    @Column()
    private Boolean candidateInterest;

    @Column()
    private Boolean landlordInterest;

    @Column()
    @FutureOrPresent
    private LocalDateTime matchDate;

    @Column(nullable = false)
    @ManyToOne
    private UserEntity candidate;
    
    @Column(nullable = false)
    @ManyToOne
    private ApartmentEntity apartment;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MatchStatus matchStatus;

    public ApartmentMatchEntity() {
    }

    public ApartmentMatchEntity(Integer id, Boolean candidateInterest, Boolean landlordInterest, LocalDateTime matchDate, UserEntity candidate, ApartmentEntity apartment) {
        this.id = id;
        this.candidateInterest = candidateInterest;
        this.landlordInterest = landlordInterest;
        this.matchDate = matchDate;
        this.candidate = candidate;
        this.apartment = apartment;
    }       

}
