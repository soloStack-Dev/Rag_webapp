package com.example.rag_app.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ChunkingService {

    public record Chunk(String text, int chunkIndex) {
    }

    public List<Chunk> chunkPages(List<PdfParsingService.ParsedPage> pages, int chunkSize, int chunkOverlap) {
        List<Chunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        for (PdfParsingService.ParsedPage page : pages) {
            for (String piece : chunkText(page.text(), chunkSize, chunkOverlap)) {
                chunks.add(new Chunk(piece, chunkIndex++));
            }
        }
        return chunks;
    }

    public List<String> chunkText(String text, int chunkSize, int chunkOverlap) {
        List<String> result = new ArrayList<>();
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return result;
        }
        String[] sentences = normalized.split("(?<=[.!?])\\s+");
        int idx = 0;
        StringBuilder current = new StringBuilder();
        while (idx < sentences.length) {
            String sentence = sentences[idx++].trim();
            if (current.isEmpty()) {
                current = new StringBuilder(sentence);
                if (current.length() > chunkSize) {
                    for (String part : hardSplit(sentence, chunkSize)) {
                        result.add(part);
                    }
                    current.setLength(0);
                }
                continue;
            }
            if (current.length() + 1 + sentence.length() <= chunkSize) {
                current.append(' ').append(sentence);
            } else {
                result.add(current.toString());
                String tail = overlapTail(current.toString(), chunkOverlap);
                current = new StringBuilder(tail.isEmpty() || tail.length() >= chunkSize ? "" : tail);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private String overlapTail(String text, int overlap) {
        if (overlap <= 0 || text.length() <= overlap) {
            return "";
        }
        int cut = text.length() - overlap;
        int space = text.indexOf(' ', cut);
        if (space < 0) {
            return text.substring(cut).trim();
        }
        return text.substring(space + 1).trim();
    }

    private List<String> hardSplit(String text, int chunkSize) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            parts.add(text.substring(start, end).trim());
            start = end;
        }
        return parts;
    }
}