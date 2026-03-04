package com.example.demo.MemberApartment;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.User.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "apartment_members")
public class ApartmentMemberEntity {

    @Id
    @SequenceGenerator(name = "apartment_member_seq", sequenceName = "apartment_member_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartment_member_seq")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "apartment_id", nullable = false)
    private ApartmentEntity apartment;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private LocalDate joinDate;

    @Column(nullable = true)
    private LocalDate leaveDate;

    public ApartmentMemberEntity() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ApartmentEntity getApartment() {
        return apartment;
    }

    public void setApartment(ApartmentEntity apartment) {
        this.apartment = apartment;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public LocalDate getLeaveDate() {
        return leaveDate;
    }

    public void setLeaveDate(LocalDate leaveDate) {
        this.leaveDate = leaveDate;
    }

    public boolean isActive() {
        return leaveDate == null || leaveDate.isAfter(LocalDate.now());
    }
}   