package com.example.demo.User;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "refresh_tokens", uniqueConstraints = {
        @UniqueConstraint(
                name="user_id_device_id",
                columnNames = {"user_id", "deviceId"}
        )
})
public class RefreshTokenEntity {

    @Id
    @SequenceGenerator(name = "rt_seq",
            sequenceName = "entity_sequence",
            initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rt_seq")
    private Integer id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private Date expiration;

    @ManyToOne(optional = true)
    private UserEntity user;

    public RefreshTokenEntity() {}

    public RefreshTokenEntity(Integer id, String token, String deviceId, Date expiration, UserEntity user) {
        this.id = id;
        this.token = token;
        this.deviceId = deviceId;
        this.expiration = expiration;
        this.user = user;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}



