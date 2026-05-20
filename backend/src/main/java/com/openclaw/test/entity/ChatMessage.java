package com.openclaw.test.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String reply;

    @Column(name = "token_usage")
    private String tokenUsage;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatMessage() {}

    public ChatMessage(Long userId, String username, String message, String reply, String tokenUsage) {
        this(userId, username, message, reply, tokenUsage, null);
    }

    public ChatMessage(Long userId, String username, String message, String reply, String tokenUsage, Long conversationId) {
        this.userId = userId;
        this.username = username;
        this.message = message;
        this.reply = reply;
        this.tokenUsage = tokenUsage;
        this.conversationId = conversationId;
        this.createdAt = LocalDateTime.now();
    }

    public ChatMessage(Long userId, String username, String message, String reply, String tokenUsage, Long conversationId, String fileUrl) {
        this.userId = userId;
        this.username = username;
        this.message = message;
        this.reply = reply;
        this.tokenUsage = tokenUsage;
        this.conversationId = conversationId;
        this.fileUrl = fileUrl;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(String tokenUsage) { this.tokenUsage = tokenUsage;
        this.conversationId = conversationId; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
