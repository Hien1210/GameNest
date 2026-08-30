package com.gamenest.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/**
 * Integration Service (CLAUDE.md §15) wrapping the Cloudinary Java SDK.
 * Replaces the old filesystem-based {@code AvatarStorage}: images are
 * uploaded to Cloudinary's CDN instead of the local disk.
 */
public final class CloudinaryUploader {

    private static final Cloudinary CLOUDINARY = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", CloudinaryConfig.getCloudName(),
            "api_key", CloudinaryConfig.getApiKey(),
            "api_secret", CloudinaryConfig.getApiSecret(),
            "secure", true
    ));

    private CloudinaryUploader() {
    }

    /**
     * Uploads an avatar image for the given account, overwriting any
     * previous avatar at the same public_id and busting the CDN cache.
     *
     * @return the absolute HTTPS CDN URL to store in {@code Accounts.avatar_url}
     */
    public static String uploadAvatar(int accountId, InputStream data) throws IOException {
        Map<?, ?> uploadResult = CLOUDINARY.uploader().upload(readAllBytes(data), ObjectUtils.asMap(
                "public_id", "gamenest/avatars/avatar_" + accountId,
                "overwrite", true,
                "invalidate", true
        ));

        Object secureUrl = uploadResult.get("secure_url");
        if (secureUrl == null) {
            throw new IOException("Cloudinary không trả về URL ảnh sau khi upload.");
        }
        return secureUrl.toString();
    }

    private static byte[] readAllBytes(InputStream data) throws IOException {
        return data.readAllBytes();
    }

    /**
     * Uploads a Chat message image attachment to a namespace dedicated to
     * Chat ({@code gamenest/chat/...}), separate from the Avatar namespace
     * ({@code gamenest/avatars/...}). Unlike {@link #uploadAvatar}, the
     * public_id here is a server-generated random UUID — never the client's
     * filename, and never overwritten (each message attachment is its own
     * permanent asset, not a "current avatar" slot).
     *
     * @return the Cloudinary {@code public_id} to persist in
     * {@code MessageAttachments.stored_file_name} (never the full URL — URL
     * construction is a later-phase concern, see {@code ChatService}).
     */
    public static String uploadChatAttachment(byte[] data) throws IOException {
        String publicId = "gamenest/chat/" + UUID.randomUUID();
        Map<?, ?> uploadResult = CLOUDINARY.uploader().upload(data, ObjectUtils.asMap(
                "public_id", publicId,
                "resource_type", "image"
        ));

        Object returnedPublicId = uploadResult.get("public_id");
        if (returnedPublicId == null) {
            throw new IOException("Cloudinary không trả về public_id sau khi upload.");
        }
        return returnedPublicId.toString();
    }

    /**
     * Compensating delete for {@link #uploadChatAttachment} — used only when
     * the Cloudinary upload itself succeeded but the subsequent DB write
     * (Message + MessageAttachments in the same transaction) failed, so no
     * orphaned asset is left behind. "not found" is treated as an acceptable
     * outcome (already gone), not a failure.
     */
    public static void deleteChatAttachment(String publicId) throws IOException {
        Map<?, ?> result = CLOUDINARY.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        Object status = result.get("result");
        if (status == null || !("ok".equals(status.toString()) || "not found".equals(status.toString()))) {
            throw new IOException("Cloudinary destroy không thành công cho public_id: " + publicId);
        }
    }
}
