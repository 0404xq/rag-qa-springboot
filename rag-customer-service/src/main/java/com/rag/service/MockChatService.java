package com.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MockChatService {

    public String generate(String prompt) {
        log.info("Mock 模式收到问题: {}", prompt);
        return "【模拟回复】\n\n这是 RAG 智能客服的模拟响应。\n\n当前系统运行正常！";
    }
}