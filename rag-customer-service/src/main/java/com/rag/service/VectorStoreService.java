package com.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUris;

    private static final String INDEX_NAME = "knowledge_vectors";

    public void createIndexIfNotExists() {
        try {
            String url = elasticsearchUris + "/" + INDEX_NAME;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("索引已存在: {}", INDEX_NAME);
                return;
            }
        } catch (Exception e) {
            log.info("索引不存在，正在创建...");
        }

        try {
            String url = elasticsearchUris + "/" + INDEX_NAME;

            String mapping = """
                {
                    "mappings": {
                        "properties": {
                            "embedding": {
                                "type": "dense_vector",
                                "dims": 384,
                                "index": true,
                                "similarity": "cosine"
                            },
                            "text": {
                                "type": "text",
                                "analyzer": "standard"
                            },
                            "metadata": {
                                "type": "object",
                                "enabled": true
                            }
                        }
                    }
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(mapping, headers);

            restTemplate.put(url, entity);
            log.info("索引创建成功: {}", INDEX_NAME);
        } catch (Exception e) {
            log.error("创建索引失败", e);
        }
    }

    public void addVector(String id, float[] embedding, String text, Map<String, Object> metadata) {
        try {
            String url = elasticsearchUris + "/" + INDEX_NAME + "/_doc/" + id;

            Map<String, Object> document = new HashMap<>();
            document.put("embedding", embedding);
            document.put("text", text);
            document.put("metadata", metadata != null ? metadata : new HashMap<>());
            document.put("timestamp", System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(document, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("向量添加成功: {}", id);
            }
        } catch (Exception e) {
            log.error("添加向量失败", e);
        }
    }

    public List<VectorMatch> searchSimilar(float[] queryEmbedding, int topK) {
        List<VectorMatch> results = new ArrayList<>();

        try {
            String url = elasticsearchUris + "/" + INDEX_NAME + "/_search";

            String query = String.format("""
                {
                    "size": %d,
                    "query": {
                        "script_score": {
                            "query": {"match_all": {}},
                            "script": {
                                "source": "cosineSimilarity(params.query_vector, 'embedding') + 1.0",
                                "params": {
                                    "query_vector": %s
                                }
                            }
                        }
                    }
                }
                """, topK, Arrays.toString(queryEmbedding));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(query, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode hits = root.path("hits").path("hits");

                for (JsonNode hit : hits) {
                    double score = hit.path("_score").asDouble() - 1.0;
                    String text = hit.path("_source").path("text").asText();
                    String id = hit.path("_id").asText();

                    VectorMatch match = new VectorMatch();
                    match.setId(id);
                    match.setText(text);
                    match.setScore(score);
                    results.add(match);
                }
            }
        } catch (Exception e) {
            log.error("搜索向量失败", e);
        }

        return results;
    }

    public void deleteVector(String id) {
        try {
            String url = elasticsearchUris + "/" + INDEX_NAME + "/_doc/" + id;
            restTemplate.delete(url);
            log.debug("向量删除成功: {}", id);
        } catch (Exception e) {
            log.error("删除向量失败", e);
        }
    }

    @lombok.Data
    public static class VectorMatch {
        private String id;
        private String text;
        private double score;
    }
}