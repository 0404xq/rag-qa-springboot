package com.rag.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatResponse {
    private String id;
    private String message;
    private String sessionId;
    private List<SourceDocument> sources;
    private Long processingTime;
    private Boolean success;
    private String error;

    @Data
    @Builder
    public static class SourceDocument {
        private String title;
        private String content;
        private Double similarity;
        private String category;
    }
}