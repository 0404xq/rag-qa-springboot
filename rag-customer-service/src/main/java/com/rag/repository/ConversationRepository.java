package com.rag.repository;

import com.rag.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<Conversation> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    List<Conversation> findByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteBySessionId(String sessionId);

    @Query(value = "SELECT DISTINCT session_id FROM conversations WHERE user_id = :userId ORDER BY created_at DESC", nativeQuery = true)
    List<String> findDistinctSessionIdsByUserId(@Param("userId") Long userId);
}