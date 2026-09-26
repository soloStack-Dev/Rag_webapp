package com.example.rag_app.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

@Service
public class PdfParsingService {

    public record ParsedPage(String text, int pageNumber) {
    }

    public List<ParsedPage> parse(Path file) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(new FileSystemResource(file));
        List<Document> documents = reader.get();
        List<ParsedPage> pages = new ArrayList<>();
        int index = 1;
        for (Document doc : documents) {
            int pageNumber = index;
            Object pageMeta = doc.getMetadata().get("page_number");
            if (pageMeta instanceof Number number) {
                pageNumber = number.intValue();
            }
            String text = clean(doc.getText());
            if (!text.isBlank()) {
                pages.add(new ParsedPage(text, pageNumber));
            }
            index++;
        }
        return pages;
    }

    private String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.replace("\u0000", " ");
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        StringBuilder cleaned = new StringBuilder();
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            cleaned.append(trimmed).append('\n');
        }
        return cleaned.toString().trim();
    }
}