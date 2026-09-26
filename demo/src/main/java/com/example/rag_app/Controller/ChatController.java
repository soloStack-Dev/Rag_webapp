package com.example.rag_app.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.rag_app.Entity.ChatMessage;
import com.example.rag_app.Entity.ChatSession;
import com.example.rag_app.Repository.ChatSessionRepository;
import com.example.rag_app.Repository.DocumentRepository;
import com.example.rag_app.Service.ChatService;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionRepository sessionRepository;
    private final DocumentRepository documentRepository;

    public ChatController(ChatService chatService, ChatSessionRepository sessionRepository,
            DocumentRepository documentRepository) {
        this.chatService = chatService;
        this.sessionRepository = sessionRepository;
        this.documentRepository = documentRepository;
    }

    @ModelAttribute("sessions")
    public List<ChatSession> sessions() {
        return sessionRepository.findAllByOrderByUpdatedAtDesc();
    }

    @ModelAttribute("documents")
    public List<com.example.rag_app.Entity.DocumentMetadata> documents() {
        return documentRepository.findTop10ByOrderByUploadedAtDesc();
    }

    @GetMapping("")
    public String chatPage() {
        return "chat";
    }

    @GetMapping("/history")
    public String history() {
        return "fragments/history :: history";
    }

    @GetMapping("/workspace")
    public String workspace(@RequestParam(required = false) Long id, Model model) {
        ChatSession session = id == null ? null : chatService.getSession(id);
        model.addAttribute("activeSession", session);
        model.addAttribute("messages", session == null ? List.<ChatMessage>of() : chatService.messages(session.getId()));
        return "fragments/workspace :: workspace";
    }

    @PostMapping("/new")
    public String newChat(Model model) {
        ChatSession session = chatService.createSession();
        model.addAttribute("activeSession", session);
        model.addAttribute("messages", List.<ChatMessage>of());
        return "fragments/workspace :: workspace";
    }

    @PostMapping("/message")
    public String message(@RequestParam Long sessionId, @RequestParam String question, Model model) {
        ChatSession session = chatService.ask(sessionId, question);
        model.addAttribute("activeSession", session);
        model.addAttribute("messages", chatService.messages(session.getId()));
        return "fragments/workspace :: workspace";
    }

    @GetMapping("/history/{id}/edit")
    public String editHome(@PathVariable Long id, Model model) {
        ChatSession session = sessionRepository.findById(id).orElse(null);
        if (session == null) {
            return "fragments/history :: history";
        }
        model.addAttribute("sess", session);
        return "fragments/history-edit :: edit-item";
    }

    @PostMapping("/history/{id}/rename")
    public String rename(@PathVariable Long id, @RequestParam String title, Model model) {
        chatService.rename(id, title);
        ChatSession session = sessionRepository.findById(id).orElse(null);
        model.addAttribute("sess", session);
        return "fragments/history-item :: item";
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        chatService.delete(id);
        return ResponseEntity.noContent().build();
    }
}