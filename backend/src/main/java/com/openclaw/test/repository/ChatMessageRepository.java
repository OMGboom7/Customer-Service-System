package com.openclaw.test.repository;

import com.openclaw.test.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ChatMessage> findAllByOrderByCreatedAtDesc();
    void deleteByIdAndUserId(Long id, Long userId);

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    List<ChatMessage> findByUserIdAndConversationIdOrderByCreatedAtAsc(Long userId, Long conversationId);
}
