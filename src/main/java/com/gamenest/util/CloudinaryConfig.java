package com.gamenest.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Externalized Cloudinary credentials, mirroring {@link DBConnection}'s
 * properties-loading pattern. Never hardcode these values in source
 * (CLAUDE.md §15) — they are read from {@code cloudinary.properties}
 * (gitignored) with an environment-variable override for deployment.
 */
public final class CloudinaryConfig {

    private static final String CLOUD_NAME;
    private static final String API_KEY;
    private static final String API_SECRET;

    static {
        Properties props = new Properties();
        try (InputStream in = CloudinaryConfig.class.getClassLoader().getResourceAsStream("cloudinary.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        CLOUD_NAME = getConfig(props, "cloudinary.cloud_name");
        API_KEY = getConfig(props, "cloudinary.api_key");
        API_SECRET = getConfig(props, "cloudinary.api_secret");
    }

    private CloudinaryConfig() {
    }

    private static String getConfig(Properties props, String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return props.getProperty(key);
    }

    public static String getCloudName() {
        return CLOUD_NAME;
    }

    public static String getApiKey() {
        return API_KEY;
    }

    public static String getApiSecret() {
        return API_SECRET;
    }
}
