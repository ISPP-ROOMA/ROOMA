package com.example.demo.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Integer> {

    List<ChatMessageEntity> findByApartmentMatchIdOrderBySentAtAsc(Integer matchId);
}
