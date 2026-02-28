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

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @SequenceGenerator(name = "user_seq", sequenceName = "user_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    private Integer id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String profileImageUrl;

    private String profileImagePublicId;

    @Column
    private String hobbies;

    @Column
    private String schedule;

    @Column
    private String profession;

    public UserEntity() {
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
}