package com.example.demo.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @SequenceGenerator(name = "user_seq", sequenceName = "user_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    private Integer id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = true)
    private String password;

    @Column
    private String authProvider; // LOCAL, GOOGLE

    @Column(unique = true)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column
    private String name;

    @Column
    private String surname;

    @Column
    private LocalDate birthDate;

    @Column
    private String phone;

    @Column
    private String gender;

    @Column
    private Boolean smoker;

    private String profileImageUrl;

    private String profileImagePublicId;

    @Column
    private String hobbies;

    @Column
    private String schedule;

    @Column
    private String profession;

    @Column
    private String accountStatus;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastConnectionAt;

    public UserEntity() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (accountStatus == null || accountStatus.isBlank()) {
            accountStatus = "ACTIVE";
        }
    }

    // Imágenes
    public UserEntity(Integer id, String email, String password, Role role, String profileImageUrl, String profileImagePublicId) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.profileImageUrl = profileImageUrl;
        this.profileImagePublicId = profileImagePublicId;
    }

    // Perfil
    public UserEntity(Integer id, String email, String password, Role role, String hobbies, String schedule, String profession) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.hobbies = hobbies;
        this.schedule = schedule;
        this.profession = profession;
    }

    
    public UserEntity(Integer id, String email, String password, Role role, String profileImageUrl, 
                      String profileImagePublicId, String hobbies, String schedule, String profession) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.profileImageUrl = profileImageUrl;
        this.profileImagePublicId = profileImagePublicId;
        this.hobbies = hobbies;
        this.schedule = schedule;
        this.profession = profession;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Boolean getSmoker() {
        return smoker;
    }

    public void setSmoker(Boolean smoker) {
        this.smoker = smoker;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getProfileImagePublicId() {
        return profileImagePublicId;
    }

    public void setProfileImagePublicId(String profileImagePublicId) {
        this.profileImagePublicId = profileImagePublicId;
    }

    public String getHobbies() {
        return hobbies;
    }

    public void setHobbies(String hobbies) {
        this.hobbies = hobbies;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastConnectionAt() {
        return lastConnectionAt;
    }

    public void setLastConnectionAt(LocalDateTime lastConnectionAt) {
        this.lastConnectionAt = lastConnectionAt;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }
}
