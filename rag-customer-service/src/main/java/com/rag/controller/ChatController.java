package com.rag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.model.ChatRequest;
import com.rag.model.ChatResponse;
import com.rag.model.Conversation;
import com.rag.model.User;
import com.rag.service.ConversationService;
import com.rag.service.RAGService;
import com.rag.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChatController {

    private final RAGService ragService;
    private final ConversationService conversationService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @PostMapping("/ask")
    public ChatResponse ask(@RequestBody ChatRequest request) {
        log.info("收到问题: {}", request.getMessage());

        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            request.setSessionId(UUID.randomUUID().toString());
        }

        ChatResponse response = ragService.answer(
                request.getMessage(),
                request.getSessionId(),
                request.getUserId()
        );

        saveConversation(request, response);
        return response;
    }

    @PostMapping(value = "/ask/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter askStream(@RequestBody ChatRequest request) {
        log.info("收到流式问题: {}, sessionId: {}", request.getMessage(), request.getSessionId());

        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            request.setSessionId(UUID.randomUUID().toString());
        }

        SseEmitter emitter = new SseEmitter(120000L);
        String sessionId = request.getSessionId();
        String userId = request.getUserId();
        String question = request.getMessage();

        StringBuilder fullAnswer = new StringBuilder();

        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data("{\"sessionId\":\"" + sessionId + "\"}"));

                ragService.answerStream(
                        question,
                        sessionId,
                        userId,
                        chunk -> {
                            try {
                                fullAnswer.append(chunk);
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data("{\"content\":\"" + escapeJson(chunk) + "\"}"));
                            } catch (IOException e) {
                                log.error("发送消息失败", e);
                            }
                        },
                        fullContent -> {
                            try {
                                ChatResponse response = ChatResponse.builder()
                                        .message(fullContent)
                                        .sessionId(sessionId)
                                        .success(true)
                                        .build();

                                ChatRequest chatRequest = new ChatRequest();
                                chatRequest.setSessionId(sessionId);
                                chatRequest.setUserId(userId);
                                chatRequest.setMessage(question);

                                saveConversation(chatRequest, response);

                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("{\"sessionId\":\"" + sessionId + "\"}"));
                                emitter.complete();
                                log.info("✅ 流式响应完成，长度: {}", fullContent.length());
                            } catch (IOException e) {
                                log.error("完成信号发送失败", e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("{\"message\":\"" + escapeJson(error) + "\"}"));
                                emitter.complete();
                            } catch (IOException e) {
                                log.error("错误信号发送失败", e);
                            }
                        }
                );
            } catch (Exception e) {
                log.error("流式处理异常", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                } catch (IOException ex) {
                    // ignore
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    @GetMapping("/sessions")
    public Map<String, Object> getUserSessions(@RequestParam(required = false) String username,
                                               @RequestParam(required = false) String userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String targetUser = userId != null && !userId.isEmpty() ? userId : username;

            if (targetUser == null || targetUser.isEmpty()) {
                response.put("success", false);
                response.put("message", "请提供 username 或 userId");
                return response;
            }

            User user = userService.findByUsername(targetUser).orElse(null);
            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在: " + targetUser);
                return response;
            }

            log.info("查询用户会话: userId={}, username={}", user.getId(), user.getUsername());

            List<Conversation> allConversations = conversationService.getConversationsByUser(user.getId());
            log.info("找到 {} 条对话记录", allConversations.size());

            if (allConversations.isEmpty()) {
                response.put("success", true);
                response.put("data", new ArrayList<>());
                return response;
            }

            Map<String, List<Conversation>> groupMap = new LinkedHashMap<>();
            for (Conversation c : allConversations) {
                String sid = c.getSessionId();
                if (sid != null && !sid.isEmpty()) {
                    groupMap.computeIfAbsent(sid, k -> new ArrayList<>()).add(c);
                }
            }

            List<Map<String, Object>> sessions = new ArrayList<>();
            for (Map.Entry<String, List<Conversation>> entry : groupMap.entrySet()) {
                List<Conversation> convs = entry.getValue();
                if (!convs.isEmpty()) {
                    Map<String, Object> sessionInfo = new HashMap<>();
                    sessionInfo.put("sessionId", entry.getKey());
                    sessionInfo.put("title", convs.get(0).getQuestion());
                    sessionInfo.put("messageCount", convs.size());
                    sessionInfo.put("lastTime", convs.get(convs.size() - 1).getCreatedAt());
                    sessions.add(sessionInfo);
                }
            }

            sessions.sort((a, b) -> {
                Object timeA = a.get("lastTime");
                Object timeB = b.get("lastTime");
                if (timeA == null && timeB == null) return 0;
                if (timeA == null) return 1;
                if (timeB == null) return -1;
                return timeB.toString().compareTo(timeA.toString());
            });

            response.put("success", true);
            response.put("data", sessions);
            log.info("找到 {} 个会话", sessions.size());

        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 恢复会话 - 获取某个 sessionId 的所有消息，并重新加载到上下文
     */
    @GetMapping("/session/{sessionId}")
    public Map<String, Object> getSessionMessages(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("恢复会话: sessionId={}", sessionId);
            List<Conversation> conversations = conversationService.getConversationsBySession(sessionId);

            // ===== 关键：把历史记录重新加载到上下文中 =====
            ragService.clearContext(sessionId);
            for (Conversation c : conversations) {
                ragService.addContextMessage(sessionId, "user", c.getQuestion());
                ragService.addContextMessage(sessionId, "assistant", c.getAnswer());
            }
            log.info("✅ 已恢复 {} 条历史消息到上下文", conversations.size());
            // ==============================================

            response.put("success", true);
            response.put("data", conversations);
            response.put("sessionId", sessionId);
            log.info("会话 {} 有 {} 条消息", sessionId, conversations.size());
        } catch (Exception e) {
            log.error("恢复会话失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @DeleteMapping("/history")
    public void clearHistory(@RequestParam String sessionId) {
        conversationService.deleteConversationsBySession(sessionId);
    }

    @DeleteMapping("/context")
    public Map<String, Object> clearContext(@RequestParam String sessionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            ragService.clearContext(sessionId);
            response.put("success", true);
            response.put("message", "上下文已清除");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    private void saveConversation(ChatRequest request, ChatResponse response) {
        try {
            User user = null;
            if (request.getUserId() != null && !request.getUserId().isEmpty()) {
                user = userService.findByUsername(request.getUserId()).orElse(null);
            }

            String sourcesJson = "[]";
            if (response.getSources() != null && !response.getSources().isEmpty()) {
                sourcesJson = objectMapper.writeValueAsString(response.getSources());
            }

            log.info("💾 保存对话: sessionId={}, userId={}, question={}",
                    request.getSessionId(),
                    request.getUserId(),
                    request.getMessage().substring(0, Math.min(20, request.getMessage().length())));

            conversationService.saveConversation(
                    request.getSessionId(),
                    request.getMessage(),
                    response.getMessage(),
                    sourcesJson,
                    user
            );
        } catch (Exception e) {
            log.error("保存对话历史失败", e);
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}