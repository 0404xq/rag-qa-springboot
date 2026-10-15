package com.rag.model;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String sessionId;
    private String userId;      // 用户名
    private Double temperature;
    private Integer maxTokens;
}