package com.example.demo.Notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Integer> {
    List<PushSubscriptionEntity> findByUserId(Integer userId);
    void deleteByEndpoint(String endpoint);
    boolean existsByEndpoint(String endpoint);
}
