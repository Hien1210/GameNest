package com.gamenest.model;

import java.io.InputStream;

/**
 * Tạm thời trong luồng upload (Servlet -> Service -> DAO) — không persist
 * DB, không phải entity. Giữ {@code InputStream} thay vì {@code byte[]} để
 * stream trực tiếp ra đĩa, không đọc toàn bộ file vào RAM.
 */
public class ChatAttachmentUpload {

    private final InputStream inputStream;
    private final String mimeType;
    private final long declaredSize;

    public ChatAttachmentUpload(InputStream inputStream, String mimeType, long declaredSize) {
        this.inputStream = inputStream;
        this.mimeType = mimeType;
        this.declaredSize = declaredSize;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getDeclaredSize() {
        return declaredSize;
    }
}
