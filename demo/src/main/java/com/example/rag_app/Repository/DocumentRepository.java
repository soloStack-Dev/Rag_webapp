package com.example.rag_app.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.rag_app.Entity.DocumentMetadata;

public interface DocumentRepository extends JpaRepository<DocumentMetadata, Long> {

    List<DocumentMetadata> findTop10ByOrderByUploadedAtDesc();
}