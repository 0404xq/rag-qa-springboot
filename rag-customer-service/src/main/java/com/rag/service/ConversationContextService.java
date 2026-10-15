package com.rag.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationContextService {

    private final Map<String, List<Map<String, String>>> contextMap = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 10;

    public void addMessage(String sessionId, String role, String content) {
        contextMap.putIfAbsent(sessionId, new ArrayList<>());
        List<Map<String, String>> history = contextMap.get(sessionId);

        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        history.add(message);

        if (history.size() > MAX_HISTORY * 2) {
            history.subList(0, history.size() - MAX_HISTORY * 2).clear();
        }
    }

    public List<Map<String, String>> getContext(String sessionId) {
        return contextMap.getOrDefault(sessionId, new ArrayList<>());
    }

    public void clearContext(String sessionId) {
        contextMap.remove(sessionId);
    }

    /**
     * 构建智能助手模式的提示词
     * - 有知识库参考：优先基于知识库，但可以补充通用知识
     * - 无知识库参考：自由回答（像 AI 助手一样）
     */
    public String buildContextualPrompt(String sessionId, String question, String knowledgeContext) {
        List<Map<String, String>> history = getContext(sessionId);

        StringBuilder prompt = new StringBuilder();

        // ===== 系统角色设定 =====
        prompt.append("你是小智，一个智能助手，具备以下能力：\n");
        prompt.append("1. 优先使用【知识库参考内容】回答用户问题，确保信息准确\n");
        prompt.append("2. 如果知识库中没有相关内容，可以用你自己的知识回答，保持专业和友好\n");
        prompt.append("3. 请根据【对话历史】理解用户的意图，回答要连贯、自然\n");
        prompt.append("4. 回答要清晰、有条理，适合用列表或分段时请使用\n\n");

        // ===== 知识库参考 =====
        boolean hasKnowledge = knowledgeContext != null
                && !knowledgeContext.isEmpty()
                && !"没有找到相关知识".equals(knowledgeContext);

        if (hasKnowledge) {
            prompt.append("【知识库参考内容】\n");
            prompt.append(knowledgeContext);
            prompt.append("\n\n");
            prompt.append("提示：请优先参考以上知识库内容回答。\n\n");
        }

        // ===== 对话历史 =====
        if (!history.isEmpty()) {
            prompt.append("【对话历史】\n");
            for (Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equals(role)) {
                    prompt.append("用户: ").append(content).append("\n");
                } else {
                    prompt.append("助手: ").append(content).append("\n");
                }
            }
            prompt.append("\n");
        }

        // ===== 当前问题 =====
        prompt.append("用户: ").append(question).append("\n\n");
        prompt.append("请回答：");

        return prompt.toString();
    }
}