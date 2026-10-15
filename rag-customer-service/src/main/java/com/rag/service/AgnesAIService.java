package com.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.config.ModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgnesAIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ModelConfig modelConfig;

    private String currentModel;

    private String getCurrentModel() {
        if (currentModel == null || currentModel.isEmpty()) {
            currentModel = modelConfig.getDefaultModel();
        }
        return currentModel;
    }

    public void setModel(String model) {
        this.currentModel = model;
        log.info("🔄 切换模型: {}", model);
    }

    public String getModel() {
        return getCurrentModel();
    }

    public String chat(String prompt) {
        try {
            String url = modelConfig.getBaseUrl() + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + modelConfig.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", getCurrentModel());
            requestBody.put("messages", new Object[]{
                    new HashMap<String, String>() {{
                        put("role", "user");
                        put("content", prompt);
                    }}
            });
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 800);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            long costTime = System.currentTimeMillis() - startTime;

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            if (jsonNode.has("error")) {
                String errorMsg = jsonNode.get("error").has("message")
                        ? jsonNode.get("error").get("message").asText()
                        : "未知错误";
                log.error("❌ AI 错误: {}", errorMsg);
                return "AI 服务错误: " + errorMsg;
            }

            if (jsonNode.has("choices") && jsonNode.get("choices").size() > 0) {
                JsonNode choice = jsonNode.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    String content = choice.get("message").get("content").asText();
                    log.info("✅ AI 响应成功，模型: {}, 耗时: {}ms", getCurrentModel(), costTime);
                    return content;
                }
            }

            return "AI 服务返回了未知响应";

        } catch (Exception e) {
            log.error("❌ 调用 AI 失败", e);
            return "调用 AI 服务时发生错误: " + e.getMessage();
        }
    }

    /**
     * 流式模式 - 修复版
     */
    public void chatStream(String prompt, Consumer<String> onMessage,
                           Consumer<String> onComplete, Consumer<String> onError) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(modelConfig.getBaseUrl() + "/chat/completions");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + modelConfig.getApiKey());
            connection.setDoOutput(true);
            connection.setReadTimeout(60000);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", getCurrentModel());
            requestBody.put("messages", new Object[]{
                    new HashMap<String, String>() {{
                        put("role", "user");
                        put("content", prompt);
                    }}
            });
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 800);
            requestBody.put("stream", true);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            connection.getOutputStream().write(jsonBody.getBytes("UTF-8"));
            connection.getOutputStream().flush();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"))) {

                    String line;
                    StringBuilder fullContent = new StringBuilder();
                    StringBuilder reasoningContent = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();

                            if ("[DONE]".equals(data)) {
                                break;
                            }

                            try {
                                JsonNode jsonNode = objectMapper.readTree(data);
                                if (jsonNode.has("choices") && jsonNode.get("choices").size() > 0) {
                                    JsonNode choice = jsonNode.get("choices").get(0);
                                    JsonNode delta = choice.get("delta");

                                    if (delta != null) {
                                        // 收集思考过程（不推送）
                                        if (delta.has("reasoning_content") && !delta.get("reasoning_content").isNull()) {
                                            reasoningContent.append(delta.get("reasoning_content").asText());
                                        }

                                        // 只推送最终内容
                                        if (delta.has("content") && !delta.get("content").isNull()) {
                                            String content = delta.get("content").asText();
                                            if (content != null && !content.isEmpty()) {
                                                fullContent.append(content);
                                                if (onMessage != null) {
                                                    onMessage.accept(content);
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // 忽略解析错误
                            }
                        }
                    }

                    // 如果只有思考过程，没有最终内容
                    String finalContent = fullContent.toString();
                    if (finalContent.isEmpty() && reasoningContent.length() > 0) {
                        log.warn("⚠️ 模型只返回了思考过程，尝试提取");
                        finalContent = reasoningContent.toString();
                        if (onMessage != null) {
                            onMessage.accept(finalContent);
                        }
                    }

                    if (onComplete != null) {
                        onComplete.accept(finalContent);
                    }
                    log.info("✅ 流式响应完成，模型: {}, 内容长度: {}, 思考长度: {}",
                            getCurrentModel(), finalContent.length(), reasoningContent.length());
                }
            } else {
                // 读取错误响应
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line);
                    }
                    String errorStr = error.toString();
                    log.error("❌ HTTP 错误: {} - {}", responseCode, errorStr);

                    // 精确区分错误类型
                    String friendlyMsg;
                    if (errorStr.contains("1305") || errorStr.contains("访问量过大")) {
                        friendlyMsg = "⚠️ 当前模型访问量过大，请稍后重试或切换其他模型";
                    } else if (responseCode == 429) {
                        friendlyMsg = "⚠️ 请求过于频繁，请稍后重试或切换其他模型";
                    } else if (responseCode == 402) {
                        friendlyMsg = "⚠️ 账户余额不足，请使用免费模型";
                    } else if (responseCode == 401) {
                        friendlyMsg = "❌ API Key 无效，请检查配置";
                    } else if (responseCode == 404) {
                        friendlyMsg = "⚠️ 当前模型不可用，请切换其他模型";
                    } else {
                        friendlyMsg = "AI 服务返回错误: " + responseCode;
                    }

                    if (onError != null) {
                        onError.accept(friendlyMsg);
                    }
                }
            }

        } catch (Exception e) {
            log.error("❌ 流式调用失败", e);
            if (onError != null) {
                onError.accept("调用 AI 服务时发生错误: " + e.getMessage());
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}