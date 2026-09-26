package com.example.rag_app.Controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.example.rag_app.Entity.DocumentMetadata;
import com.example.rag_app.Repository.DocumentRepository;
import com.example.rag_app.Service.DocumentIngestionService;

@Controller
public class DocumentController {

    public static final long MAX_FILE_BYTES = 20L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final DocumentIngestionService ingestionService;

    public DocumentController(DocumentRepository documentRepository, DocumentIngestionService ingestionService) {
        this.documentRepository = documentRepository;
        this.ingestionService = ingestionService;
    }

    @ModelAttribute("documents")
    public List<DocumentMetadata> documents() {
        return documentRepository.findTop10ByOrderByUploadedAtDesc();
    }

    @PostMapping("/documents/upload")
    public String upload(@RequestParam("file") MultipartFile file, Model model) {
        String error = validate(file);
        if (error != null) {
            model.addAttribute("uploadError", error);
            return "fragments/documents :: documents";
        }
        String fileName = safeFileName(file.getOriginalFilename());
        DocumentMetadata doc = ingestionService.createUpload(fileName);
        try {
            ingestionService.processAsync(doc.getId(), fileName, file.getBytes());
        } catch (Exception e) {
            doc.setStatus(DocumentIngestionService.STATUS_FAILED);
            doc.setErrorMessage("Could not read the uploaded file.");
            documentRepository.save(doc);
        }
        return "fragments/documents :: documents";
    }

    @GetMapping("/documents/{id}/status")
    public String status(@PathVariable Long id, Model model) {
        DocumentMetadata doc = documentRepository.findById(id).orElse(null);
        if (doc == null) {
            model.addAttribute("uploadError", "This document upload is no longer tracked.");
        } else {
            model.addAttribute("doc", doc);
        }
        return "fragments/document-item :: doc-item";
    }

    @DeleteMapping("/documents/{id}")
    public String delete(@PathVariable Long id) {
        ingestionService.delete(id);
        return "fragments/documents :: documents";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleTooLarge(Model model) {
        model.addAttribute("uploadError", "The uploaded file is too large. Maximum size is 20 MB.");
        return "fragments/documents :: documents";
    }

    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Choose a PDF file to upload.";
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        boolean isPdf = (contentType != null && contentType.equalsIgnoreCase("application/pdf"))
                || (name != null && name.toLowerCase().endsWith(".pdf"));
        if (!isPdf) {
            return "Only PDF files are allowed.";
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            return "The uploaded file is too large. Maximum size is 20 MB.";
        }
        return null;
    }

    private String safeFileName(String originalName) {
        String name = originalName == null ? "document.pdf" : originalName;
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return sanitized.isEmpty() ? "document.pdf" : sanitized;
    }
}