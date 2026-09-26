package com.example.rag_app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EmbeddingVectorStoreSmokeTests {

    @Autowired
    EmbeddingModel embeddingModel;

    @Autowired
    VectorStore vectorStore;

    @Test
    void embedAndSearchEndToEnd() {
        float[] single = embeddingModel.embed("the capital of France is Paris");
        assertThat(single).isNotNull().hasSizeGreaterThan(0);

        List<float[]> batch = embeddingModel.embed(List.of("planets orbit the sun", "dogs love to chase balls"));
        assertThat(batch).hasSize(2);
        assertThat(batch.get(0)).hasSizeGreaterThan(0);

        Document paris = new Document("Paris is the capital of France and the largest city in the country.",
                Map.of("sourceFile", "smoke-test.pdf", "pageNumber", 1));
        Document cats = new Document("Cats are independent pets that sleep up to 16 hours a day.",
                Map.of("sourceFile", "smoke-test.pdf", "pageNumber", 2));
        vectorStore.add(List.of(paris, cats));

        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder().query("What is the capital of France?").topK(2).build());
            assertThat(results).isNotEmpty();
            assertThat(results).anyMatch(doc -> doc.getText().contains("Paris"));

            List<Document> catResults = vectorStore.similaritySearch(
                    SearchRequest.builder().query("How many hours do cats sleep?").topK(2).build());
            assertThat(catResults).isNotEmpty();
            assertThat(catResults).anyMatch(doc -> doc.getText().contains("Cats"));
        } finally {
            List<String> ids = new ArrayList<>();
            ids.add(paris.getId());
            ids.add(cats.getId());
            vectorStore.delete(ids);
        }
    }
}