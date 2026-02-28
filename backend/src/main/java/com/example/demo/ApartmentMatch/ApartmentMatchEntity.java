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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.FutureOrPresent;

@Entity
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

    @JoinColumn(name = "candidate_id")
    @ManyToOne
    private UserEntity candidate;
    
    @JoinColumn(name = "apartment_id")
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
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean getCandidateInterest() {
        return candidateInterest;
    }

    public void setCandidateInterest(Boolean candidateInterest) {
        this.candidateInterest = candidateInterest;
    }

    public Boolean getLandlordInterest() {
        return landlordInterest;
    }

    public void setLandlordInterest(Boolean landlordInterest) {
        this.landlordInterest = landlordInterest;
    }

    public LocalDateTime getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(LocalDateTime matchDate) {
        this.matchDate = matchDate;
    }

    public UserEntity getCandidate() {
        return candidate;
    }

    public void setCandidate(UserEntity candidate) {
        this.candidate = candidate;
    }

    public ApartmentEntity getApartment() {
        return apartment;
    }

    public void setApartment(ApartmentEntity apartment) {
        this.apartment = apartment;
    }

    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(MatchStatus matchStatus) {
        this.matchStatus = matchStatus;
    }

}
