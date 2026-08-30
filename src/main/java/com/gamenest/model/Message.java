package com.gamenest.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class Message {

    private int messageId;
    private int conversationId;
    private int senderAccountId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    private LocalDateTime deletedAt;
    private Integer replyToMessageId;

    // Populated only by DAO queries that join Accounts for display — not
    // persisted columns on this entity.
    private String senderUsername;
    private String senderDisplayName;
    private String senderAvatarUrl;

    // Populated only by DAO queries that LEFT JOIN the reply target message
    // (Reply feature) — not persisted columns on this entity. Null unless
    // replyToMessageId is non-null.
    private Integer replyToSenderAccountId;
    private String replyToSenderUsername;
    private String replyToSenderDisplayName;
    private String replyToContent;
    private LocalDateTime replyToDeletedAt;

    // Populated only by ChatService.listMessages' batch enrichment step
    // (Reaction feature, MessageReactionDAO) — not persisted columns on
    // this entity, and never populated by MessageDAO.mapRow itself since
    // reactions are 1-to-many per message (cannot be represented via
    // MessageDAO's single-row LEFT JOIN pattern without row multiplication).
    // reactions defaults to an empty list (never null) so JSP/JS never need
    // a null-check; myReaction is null when the caller has not reacted.
    private List<MessageReactionSummary> reactions = Collections.emptyList();
    private String myReaction;

    // Populated only by DAO queries that LEFT JOIN MessageAttachments —
    // not a persisted column on this entity. Both null unless the message
    // has an attachment.
    private String attachmentMimeType;
    private Long attachmentSizeBytes;

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public int getSenderAccountId() {
        return senderAccountId;
    }

    public void setSenderAccountId(int senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }

    public Integer getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(Integer replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public Integer getReplyToSenderAccountId() {
        return replyToSenderAccountId;
    }

    public void setReplyToSenderAccountId(Integer replyToSenderAccountId) {
        this.replyToSenderAccountId = replyToSenderAccountId;
    }

    public String getReplyToSenderUsername() {
        return replyToSenderUsername;
    }

    public void setReplyToSenderUsername(String replyToSenderUsername) {
        this.replyToSenderUsername = replyToSenderUsername;
    }

    public String getReplyToSenderDisplayName() {
        return replyToSenderDisplayName;
    }

    public void setReplyToSenderDisplayName(String replyToSenderDisplayName) {
        this.replyToSenderDisplayName = replyToSenderDisplayName;
    }

    public String getReplyToContent() {
        return replyToContent;
    }

    public void setReplyToContent(String replyToContent) {
        this.replyToContent = replyToContent;
    }

    public LocalDateTime getReplyToDeletedAt() {
        return replyToDeletedAt;
    }

    public void setReplyToDeletedAt(LocalDateTime replyToDeletedAt) {
        this.replyToDeletedAt = replyToDeletedAt;
    }

    public List<MessageReactionSummary> getReactions() {
        return reactions;
    }

    public void setReactions(List<MessageReactionSummary> reactions) {
        this.reactions = reactions == null ? Collections.emptyList() : reactions;
    }

    public String getMyReaction() {
        return myReaction;
    }

    public void setMyReaction(String myReaction) {
        this.myReaction = myReaction;
    }

    public String getAttachmentMimeType() {
        return attachmentMimeType;
    }

    public void setAttachmentMimeType(String attachmentMimeType) {
        this.attachmentMimeType = attachmentMimeType;
    }

    public Long getAttachmentSizeBytes() {
        return attachmentSizeBytes;
    }

    public void setAttachmentSizeBytes(Long attachmentSizeBytes) {
        this.attachmentSizeBytes = attachmentSizeBytes;
    }
}
