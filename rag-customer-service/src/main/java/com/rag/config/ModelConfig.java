package com.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "zhipu")
public class ModelConfig {

    private String apiKey;
    private String baseUrl;
    private String defaultModel;
    private List<ModelInfo> models = new ArrayList<>();

    @Data
    public static class ModelInfo {
        private String id;
        private String name;
    }
}
