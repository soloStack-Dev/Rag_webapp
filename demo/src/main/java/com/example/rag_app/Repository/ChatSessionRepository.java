package com.example.rag_app.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.rag_app.Entity.ChatSession;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findAllByOrderByUpdatedAtDesc();
}