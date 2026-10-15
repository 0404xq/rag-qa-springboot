package com.rag.service;

import com.rag.model.ChatResponse;
import com.rag.service.VectorStoreService.VectorMatch;
import com.rag.util.SimpleEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {

    private final SimpleEmbeddingModel embeddingModel;
    private final VectorStoreService vectorStoreService;
    private final AgnesAIService agnesAIService;
    private final ConversationContextService contextService;

    @Value("${rag.top-k:3}")
    private int topK;

    @Value("${rag.similarity.threshold:0.5}")
    private double similarityThreshold;

    public ChatResponse answer(String question, String sessionId, String userId) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("📝 收到问题 (用户: {}): {}", userId != null ? userId : "游客", question);

            float[] questionEmbedding = embeddingModel.embed(question);
            List<VectorMatch> matches = vectorStoreService.searchSimilar(questionEmbedding, topK);

            List<VectorMatch> relevantMatches = matches.stream()
                    .filter(match -> match.getScore() >= similarityThreshold)
                    .collect(Collectors.toList());

            log.info("📚 检索到 {} 条相关文档", relevantMatches.size());

            String knowledgeContext = buildContext(relevantMatches);
            String prompt = contextService.buildContextualPrompt(sessionId, question, knowledgeContext);

            String answer = agnesAIService.chat(prompt);

            contextService.addMessage(sessionId, "user", question);
            contextService.addMessage(sessionId, "assistant", answer);

            List<ChatResponse.SourceDocument> sources = relevantMatches.stream()
                    .map(match -> ChatResponse.SourceDocument.builder()
                            .content(match.getText())
                            .similarity(match.getScore())
                            .build())
                    .collect(Collectors.toList());

            long processingTime = System.currentTimeMillis() - startTime;

            return ChatResponse.builder()
                    .message(answer)
                    .sessionId(sessionId)
                    .sources(sources)
                    .processingTime(processingTime)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("❌ RAG 问答失败", e);
            return ChatResponse.builder()
                    .message("抱歉，处理您的问题时出现错误: " + e.getMessage())
                    .sessionId(sessionId)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    public void answerStream(String question, String sessionId, String userId,
                             Consumer<String> onMessage,
                             Consumer<String> onComplete,
                             Consumer<String> onError) {
        try {
            log.info("📝 收到流式问题 (用户: {}): {}", userId != null ? userId : "游客", question);

            float[] questionEmbedding = embeddingModel.embed(question);
            List<VectorMatch> matches = vectorStoreService.searchSimilar(questionEmbedding, topK);

            List<VectorMatch> relevantMatches = matches.stream()
                    .filter(match -> match.getScore() >= similarityThreshold)
                    .collect(Collectors.toList());

            log.info("📚 检索到 {} 条相关文档", relevantMatches.size());

            String knowledgeContext = buildContext(relevantMatches);
            String prompt = contextService.buildContextualPrompt(sessionId, question, knowledgeContext);

            contextService.addMessage(sessionId, "user", question);

            StringBuilder fullAnswer = new StringBuilder();
            agnesAIService.chatStream(prompt,
                    chunk -> {
                        fullAnswer.append(chunk);
                        if (onMessage != null) {
                            onMessage.accept(chunk);
                        }
                    },
                    fullContent -> {
                        contextService.addMessage(sessionId, "assistant", fullContent);
                        if (onComplete != null) {
                            onComplete.accept(fullContent);
                        }
                    },
                    onError
            );

        } catch (Exception e) {
            log.error("❌ 流式 RAG 问答失败", e);
            if (onError != null) {
                onError.accept("处理您的问题时发生错误: " + e.getMessage());
            }
        }
    }

    public void clearContext(String sessionId) {
        contextService.clearContext(sessionId);
        log.info("🗑️ 已清除会话 {} 的上下文", sessionId);
    }

    public void addContextMessage(String sessionId, String role, String content) {
        contextService.addMessage(sessionId, role, content);
    }

    private String buildContext(List<VectorMatch> matches) {
        if (matches.isEmpty()) {
            return "没有找到相关知识";
        }
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            VectorMatch match = matches.get(i);
            context.append(i + 1).append(". ")
                    .append(match.getText())
                    .append("\n");
        }
        return context.toString();
    }
}