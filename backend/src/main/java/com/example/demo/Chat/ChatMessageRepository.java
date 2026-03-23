package com.example.demo.Chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Integer> {

    List<ChatMessageEntity> findByApartmentMatchIdOrderBySentAtAsc(Integer matchId);

    List<ChatMessageEntity> findByIncidentIdOrderBySentAtAsc(Integer incidentId);
}
