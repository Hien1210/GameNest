package com.gamenest.model;

/**
 * Search Message result row (APPROVED design, Phương án B): pairs a matched
 * {@link Message} with the pagination page (ChatService's own PAGE_SIZE,
 * same paging {@code listMessages}/chat-detail.jsp already use) it falls on
 * within its conversation's full message history — computed once here so a
 * click can redirect straight to
 * {@code chat/detail?id=...&page=...&highlight=...} without a second
 * round-trip to resolve the page.
 */
public class MessageSearchResult {

    private final Message message;
    private final int page;

    public MessageSearchResult(Message message, int page) {
        this.message = message;
        this.page = page;
    }

    public Message getMessage() {
        return message;
    }

    public int getPage() {
        return page;
    }
}
