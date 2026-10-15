/**package com.rag.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class InMemoryVectorStore {

    private final Map<String, VectorEntry> store = new ConcurrentHashMap<>();

    public void addVector(String id, float[] embedding, String text, Map<String, Object> metadata) {
        VectorEntry entry = new VectorEntry();
        entry.setEmbedding(embedding);
        entry.setText(text);
        entry.setMetadata(metadata);
        store.put(id, entry);
        log.debug("向量添加成功: {}", id);
    }

    public List<VectorMatch> searchSimilar(float[] queryEmbedding, int topK) {
        List<VectorMatch> results = new ArrayList<>();

        for (Map.Entry<String, VectorEntry> entry : store.entrySet()) {
            double similarity = cosineSimilarity(queryEmbedding, entry.getValue().getEmbedding());
            VectorMatch match = new VectorMatch();
            match.setId(entry.getKey());
            match.setText(entry.getValue().getText());
            match.setScore(similarity);
            results.add(match);
        }

        // 按相似度降序排序
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 返回前 topK 个
        return results.stream().limit(topK).collect(java.util.stream.Collectors.toList());
    }

    public void deleteVector(String id) {
        store.remove(id);
        log.debug("向量删除成功: {}", id);
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Data
    public static class VectorEntry {
        private float[] embedding;
        private String text;
        private Map<String, Object> metadata;
    }

    @Data
    public static class VectorMatch {
        private String id;
        private String text;
        private double score;
    }
}**/