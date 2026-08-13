package com.gamenest.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

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
}
