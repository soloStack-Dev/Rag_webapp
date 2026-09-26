package com.example.rag_app.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.rag_app.Config.RagProperties;
import com.example.rag_app.Entity.DocumentMetadata;
import com.example.rag_app.Repository.DocumentRepository;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    public static final String STATUS_UPLOADED = "UPLOADED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_INDEXED = "INDEXED";
    public static final String STATUS_FAILED = "FAILED";

    private final DocumentRepository documentRepository;
    private final PdfParsingService parsingService;
    private final ChunkingService chunkingService;
    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public DocumentIngestionService(DocumentRepository documentRepository, PdfParsingService parsingService,
            ChunkingService chunkingService, VectorStore vectorStore, RagProperties ragProperties) {
        this.documentRepository = documentRepository;
        this.parsingService = parsingService;
        this.chunkingService = chunkingService;
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
    }

    /**
     * Create a new document record in the UPLOADED state.
     */
    public DocumentMetadata createUpload(String fileName) {
        DocumentMetadata document = new DocumentMetadata();
        document.setFileName(fileName);
        document.setStatus(STATUS_UPLOADED);
        document.setChunkCount(0);
        document.setPageCount(0);
        return documentRepository.save(document);
    }

    /**
     * Parse a PDF, split it into chunks, and push the content to the vector store.
     */
    @Async("documentTaskExecutor")
    public void processAsync(Long documentId, String fileName, byte[] content) {
        DocumentMetadata document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return;
        }

        Path tempFile = null;
        try {
            document.setStatus(STATUS_PROCESSING);
            documentRepository.save(document);

            tempFile = Files.createTempFile("meminfo-", ".pdf");
            Files.write(tempFile, content);

            List<PdfParsingService.ParsedPage> pages = parsingService.parse(tempFile);
            if (pages.isEmpty()) {
                throw new IllegalStateException("No readable text found in the PDF.");
            }

            List<Document> chunks = buildChunks(documentId, fileName, pages);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("The PDF contains no indexable text.");
            }

            vectorStore.add(chunks);

            document.setStatus(STATUS_INDEXED);
            document.setPageCount(pages.size());
            document.setChunkCount(chunks.size());
            document.setVectorIds(chunks.stream().map(Document::getId).collect(Collectors.joining("\n")));
            document.setErrorMessage(null);
            documentRepository.save(document);

            log.info("Document {} indexed: {} chunks from {} pages", documentId, chunks.size(), pages.size());
        } catch (Exception e) {
            log.error("Document indexing failed for id={}", documentId, e);
            document.setStatus(STATUS_FAILED);
            document.setErrorMessage(safeErrorMessage(e));
            documentRepository.save(document);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    /**
     * Remove the SQL record and every vector created for that document.
     */
    public void delete(Long documentId) {
        documentRepository.findById(documentId).ifPresent(document -> {
            if (document.getVectorIds() != null && !document.getVectorIds().isBlank()) {
                vectorStore.delete(List.of(document.getVectorIds().split("\\R")));
            }
            documentRepository.delete(document);
        });
    }

    private List<Document> buildChunks(Long documentId, String fileName, List<PdfParsingService.ParsedPage> pages) {
        List<Document> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (PdfParsingService.ParsedPage page : pages) {
            List<String> pieces = chunkingService.chunkText(
                    page.text(),
                    ragProperties.getChunkSize(),
                    ragProperties.getChunkOverlap());

            for (String piece : pieces) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("sourceFile", fileName);
                metadata.put("pageNumber", page.pageNumber());
                metadata.put("documentId", documentId);
                metadata.put("chunkIndex", chunkIndex);
                metadata.put("uploadedAt", java.time.Instant.now().toString());

                chunks.add(new Document(piece, metadata));
                chunkIndex++;
            }
        }

        return chunks;
    }

    private void deleteQuietly(Path tempFile) {
        if (tempFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
            // Best effort cleanup for temp PDF files.
        }
    }

    private String safeErrorMessage(Exception e) {
        if (e instanceof IllegalStateException || e.getMessage() == null) {
            return "Document indexing failed.";
        }

        String message = e.getMessage().replace('\n', ' ').trim();
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}