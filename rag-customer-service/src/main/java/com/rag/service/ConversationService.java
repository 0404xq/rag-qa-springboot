package com.rag.service;

import com.rag.model.Conversation;
import com.rag.model.User;
import com.rag.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public void saveConversation(String sessionId, String question, String answer, String sources, User user) {
        Conversation conversation = new Conversation();
        conversation.setSessionId(sessionId);
        conversation.setQuestion(question);
        conversation.setAnswer(answer);
        conversation.setSources(sources);
        conversation.setUser(user);
        conversation.setCreatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        log.info("✅ 对话保存成功: sessionId={}, userId={}", sessionId, user != null ? user.getUsername() : "null");
    }

    public List<Conversation> getConversationsBySession(String sessionId) {
        return conversationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public List<Conversation> getConversationsByUser(Long userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void deleteConversationsBySession(String sessionId) {
        conversationRepository.deleteBySessionId(sessionId);
    }

    public List<String> findDistinctSessionIdsByUserId(Long userId) {
        return conversationRepository.findDistinctSessionIdsByUserId(userId);
    }
}