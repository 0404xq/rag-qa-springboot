package com.rag.service;

import com.rag.model.KnowledgeDocument;
import com.rag.repository.DocumentRepository;
import com.rag.util.SimpleEmbeddingModel;
import com.rag.util.TextSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final SimpleEmbeddingModel embeddingModel;
    private final VectorStoreService vectorStoreService;
    private final TextSplitter textSplitter;

    public void initIndex() {
        vectorStoreService.createIndexIfNotExists();
        log.info("✅ Elasticsearch 索引初始化完成");
    }

    public KnowledgeDocument addDocument(String title, String content, String category, List<String> tags) {
        try {
            List<String> chunks = textSplitter.split(content, 500, 50);

            for (String chunk : chunks) {
                float[] embedding = embeddingModel.embed(chunk);
                String id = UUID.randomUUID().toString();

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("title", title);
                metadata.put("category", category);
                metadata.put("tags", tags);

                vectorStoreService.addVector(id, embedding, chunk, metadata);
            }

            KnowledgeDocument document = KnowledgeDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .title(title)
                    .content(content)
                    .category(category)
                    .tags(tags)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return documentRepository.save(document);
        } catch (Exception e) {
            log.error("添加文档失败: {}", e.getMessage());
            return null;
        }
    }

    public void batchImportDocuments(List<KnowledgeDocument> documents) {
        int successCount = 0;
        for (KnowledgeDocument doc : documents) {
            KnowledgeDocument result = addDocument(
                    doc.getTitle(),
                    doc.getContent(),
                    doc.getCategory(),
                    doc.getTags()
            );
            if (result != null) {
                successCount++;
            }
        }
        log.info("✅ 成功导入 {} / {} 条文档", successCount, documents.size());
    }

    public void deleteDocument(String id) {
        documentRepository.deleteById(id);
        vectorStoreService.deleteVector(id);
    }

    public List<KnowledgeDocument> searchDocuments(String keyword) {
        return documentRepository.findByContentContaining(keyword);
    }

    public List<KnowledgeDocument> getAllDocuments() {
        Iterable<KnowledgeDocument> iterable = documentRepository.findAll();
        List<KnowledgeDocument> results = new ArrayList<>();
        iterable.forEach(results::add);
        return results;
    }
}