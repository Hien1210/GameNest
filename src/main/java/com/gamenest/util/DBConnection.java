package com.gamenest.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DBConnection {

    private static final String URL;
    private static final String USERNAME;
    private static final String PASSWORD;
    private static final String CHAT_ATTACHMENT_UPLOAD_DIR;

    static {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        URL = getConfig(props, "db.url");
        USERNAME = getConfig(props, "db.username");
        PASSWORD = getConfig(props, "db.password");
        CHAT_ATTACHMENT_UPLOAD_DIR = getConfig(props, "chat.attachment.upload.dir");

        // Force-load the driver instead of relying on ServiceLoader
        // auto-discovery, which is unreliable across the multiple
        // classloaders a servlet container uses.
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() {
    }

    private static String getConfig(Properties props, String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return props.getProperty(key);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    public static String getChatAttachmentUploadDirectory() {
        return CHAT_ATTACHMENT_UPLOAD_DIR;
    }
}
