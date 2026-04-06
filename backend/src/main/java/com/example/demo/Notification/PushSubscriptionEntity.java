package com.example.demo.Notification;

import com.example.demo.User.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "push_subscriptions")
public class PushSubscriptionEntity {

    @Id
    @SequenceGenerator(name = "push_sub_seq", sequenceName = "push_sub_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "push_sub_seq")
    private Integer id;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String endpoint;

    @NotBlank
    @Column(nullable = false)
    private String p256dh;

    @NotBlank
    @Column(nullable = false)
    private String auth;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(nullable = false)
    private UserEntity user;

    public PushSubscriptionEntity() {
    }

    public PushSubscriptionEntity(String endpoint, String p256dh, String auth, UserEntity user) {
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.user = user;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getP256dh() { return p256dh; }
    public void setP256dh(String p256dh) { this.p256dh = p256dh; }

    public String getAuth() { return auth; }
    public void setAuth(String auth) { this.auth = auth; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
}
