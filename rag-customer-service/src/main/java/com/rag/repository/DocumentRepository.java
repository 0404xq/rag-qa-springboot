package com.rag.repository;

import com.rag.model.KnowledgeDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends ElasticsearchRepository<KnowledgeDocument, String> {

    List<KnowledgeDocument> findByContentContaining(String keyword);

    List<KnowledgeDocument> findByCategory(String category);

    List<KnowledgeDocument> findByTagsContaining(String tag);
}