package com.example.rag_app.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.rag_app.Entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}