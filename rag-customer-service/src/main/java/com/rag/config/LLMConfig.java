package com.rag.config;

import com.rag.service.MockChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class LLMConfig {

    @Bean
    public MockChatService mockChatService() {
        log.info("使用 Mock Chat Model (模拟模式)");
        return new MockChatService();
    }
}