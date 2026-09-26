package com.example.rag_app.Service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.rag_app.Config.RagProperties;
import com.example.rag_app.Entity.ChatMessage;
import com.example.rag_app.Entity.ChatSession;
import com.example.rag_app.Repository.ChatMessageRepository;
import com.example.rag_app.Repository.ChatSessionRepository;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String DEFAULT_TITLE = "New Chat";
    private static final int TITLE_MAX_LENGTH = 60;
    private static final String ERROR_ANSWER = "I couldn't complete that search because the AI service is temporarily unavailable. Please try again in a moment.";

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final VectorStore vectorStore;
    private final GeminiService geminiService;
    private final RagProperties ragProperties;

    public ChatService(ChatSessionRepository sessionRepository, ChatMessageRepository messageRepository,
            VectorStore vectorStore, GeminiService geminiService, RagProperties ragProperties) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.vectorStore = vectorStore;
        this.geminiService = geminiService;
        this.ragProperties = ragProperties;
    }

    /**
     * Create a fresh chat session with the default title.
     */
    @Transactional
    public ChatSession createSession() {
        ChatSession session = new ChatSession();
        session.setTitle(DEFAULT_TITLE);
        return sessionRepository.save(session);
    }

    /**
     * Read one session by id.
     */
    @Transactional(readOnly = true)
    public ChatSession getSession(Long id) {
        return sessionRepository.findById(id).orElse(null);
    }

    /**
     * Return all messages for a session in chronological order.
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> messages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * Rename a chat session using a safe, trimmed title.
     */
    @Transactional
    public void rename(Long id, String title) {
        sessionRepository.findById(id).ifPresent(session -> {
            String cleanTitle = normalizeTitle(title);
            session.setTitle(cleanTitle);
        });
    }

    /**
     * Remove a session and its related messages.
     */
    @Transactional
    public void delete(Long id) {
        messageRepository.deleteAll(messageRepository.findBySessionIdOrderByCreatedAtAsc(id));
        sessionRepository.deleteById(id);
    }

    /**
     * Ask a question and persist both the user and assistant replies.
     */
    @Transactional
    public ChatSession ask(Long sessionId, String question) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found."));

        String cleanQuestion = normalizeQuestion(question);
        if (cleanQuestion.isEmpty()) {
            throw new IllegalArgumentException("Question must not be empty.");
        }

        if (DEFAULT_TITLE.equals(session.getTitle())) {
            session.setTitle(deriveTitle(cleanQuestion));
        }

        session.addMessage(buildUserMessage(cleanQuestion));

        String answerText;
        List<String> sourceLines;
        try {
            if (isCasualConversation(cleanQuestion)) {
                answerText = geminiService.conversation(cleanQuestion);
                sourceLines = List.of();
            } else {
                List<Document> relevantDocuments = retrieveDocuments(cleanQuestion);
                answerText = geminiService.answer(cleanQuestion, relevantDocuments);
                sourceLines = geminiService.sourceLines(relevantDocuments);
            }
        } catch (Exception e) {
            log.error("Chat request failed for session {}", sessionId, e);
            answerText = e instanceof IllegalStateException && GeminiService.QUOTA_ANSWER.equals(e.getMessage())
                    ? GeminiService.QUOTA_ANSWER
                    : ERROR_ANSWER;
            sourceLines = List.of();
        }

        session.addMessage(buildAssistantMessage(answerText, sourceLines));
        return sessionRepository.save(session);
    }

    private String normalizeQuestion(String question) {
        return question == null ? "" : question.trim();
    }

    private boolean isCasualConversation(String question) {
        String normalized = question.toLowerCase().replaceAll("\\s+", " ").trim();
        return normalized.matches(
                "^(hi|hello|hey|good morning|good afternoon|good evening|how are you|who are you|what can you do|thanks|thank you|bye|goodbye|tell me a joke|make me laugh)[!.? ]*$");
    }

    private String normalizeTitle(String title) {
        String clean = title == null ? "" : title.trim();
        return clean.isEmpty() ? DEFAULT_TITLE : clean;
    }

    private ChatMessage buildUserMessage(String question) {
        ChatMessage message = new ChatMessage();
        message.setRole("USER");
        message.setContent(question);
        return message;
    }

    private ChatMessage buildAssistantMessage(String answerText, List<String> sourceLines) {
        ChatMessage message = new ChatMessage();
        message.setRole("ASSISTANT");
        message.setContent(answerText);
        message.setSources(String.join("|", sourceLines));
        return message;
    }

    private List<Document> retrieveDocuments(String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(ragProperties.getTopK())
                .similarityThreshold(ragProperties.getSimilarityThreshold())
                .build();
        return vectorStore.similaritySearch(request);
    }

    private String deriveTitle(String question) {
        String normalized = question.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= TITLE_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, TITLE_MAX_LENGTH).trim() + "…";
    }
}