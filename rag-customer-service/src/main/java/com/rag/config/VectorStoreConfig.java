package com.rag.config;

import com.rag.util.SimpleEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Slf4j
public class VectorStoreConfig {

    @Value("${rag.embedding.dimension}")
    private int dimension;

    @Bean
    public SimpleEmbeddingModel embeddingModel() {
        log.info("初始化 Embedding 模型: 简单向量模型 (Mock模式), 维度: {}", dimension);
        return new SimpleEmbeddingModel(dimension);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}