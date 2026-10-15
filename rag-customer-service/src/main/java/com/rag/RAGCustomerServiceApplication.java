package com.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories
public class RAGCustomerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RAGCustomerServiceApplication.class, args);
    }
}