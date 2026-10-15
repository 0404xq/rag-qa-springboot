/**package com.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BaiduAIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${baidu.ai.api-key}")
    private String apiKey;

    @Value("${baidu.ai.secret-key}")
    private String secretKey;

    private String accessToken;
    private long tokenExpireTime;

    private synchronized String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }

        try {
            String url = "https://aip.baidubce.com/oauth/2.0/token" +
                    "?grant_type=client_credentials" +
                    "&client_id=" + apiKey +
                    "&client_secret=" + secretKey;

            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            if (jsonNode.has("access_token")) {
                accessToken = jsonNode.get("access_token").asText();
                int expiresIn = jsonNode.get("expires_in").asInt();
                tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
                log.info("✅ 百度 Access Token 获取成功");
                return accessToken;
            } else {
                log.error("❌ 获取 Token 失败: {}", jsonNode);
                return null;
            }
        } catch (Exception e) {
            log.error("❌ 获取百度 Access Token 失败", e);
            return null;
        }
    }

    public String chat(String prompt) {
        return chat(prompt, "completions");
    }

    public String chat(String prompt, String model) {
        try {
            String token = getAccessToken();
            if (token == null) {
                return "⚠️ 获取百度 AI Token 失败，请检查 API Key 配置";
            }

            // 使用正确的百度文心 API 地址
            String url = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions?access_token=" + token;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messages", new Object[]{
                    new HashMap<String, String>() {{
                        put("role", "user");
                        put("content", prompt);
                    }}
            });
            requestBody.put("temperature", 0.7);
            requestBody.put("top_p", 0.8);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            long costTime = System.currentTimeMillis() - startTime;

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            // 检查是否有错误
            if (jsonNode.has("error_code")) {
                int errorCode = jsonNode.get("error_code").asInt();
                String errorMsg = jsonNode.has("error_msg") ? jsonNode.get("error_msg").asText() : "未知错误";
                log.error("❌ 百度 AI 错误码: {}, 错误信息: {}", errorCode, errorMsg);

                // 错误码 110: Access token invalid or no longer valid
                if (errorCode == 110) {
                    accessToken = null; // 清除缓存的 token，下次重新获取
                    return "Token 已失效，请检查 API Key 和 Secret Key 是否正确";
                }
                return "AI 服务错误: " + errorMsg + " (错误码: " + errorCode + ")";
            }

            if (jsonNode.has("result")) {
                log.info("✅ 百度 AI 响应成功，耗时: {}ms", costTime);
                return jsonNode.get("result").asText();
            } else {
                log.error("❌ 百度 AI 未知响应: {}", jsonNode);
                return "AI 服务返回了未知响应";
            }
        } catch (Exception e) {
            log.error("❌ 调用百度 AI 失败", e);
            return "调用 AI 服务时发生错误: " + e.getMessage();
        }
    }
}**/