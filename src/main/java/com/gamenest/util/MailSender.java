package com.gamenest.util;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public final class MailSender {

    private static final String HOST;
    private static final String PORT;
    private static final String USERNAME;
    private static final String PASSWORD;
    private static final String FROM_NAME;

    private static final Session SESSION;

    static {
        Properties props = new Properties();
        try (InputStream in = MailSender.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        HOST = getConfig(props, "mail.smtp.host");
        PORT = getConfig(props, "mail.smtp.port");
        USERNAME = getConfig(props, "mail.username");
        PASSWORD = getConfig(props, "mail.password");
        FROM_NAME = getConfig(props, "mail.from.name");

        Properties smtpProps = new Properties();
        smtpProps.put("mail.smtp.auth", "true");
        smtpProps.put("mail.smtp.starttls.enable", "true");
        smtpProps.put("mail.smtp.host", HOST);
        smtpProps.put("mail.smtp.port", PORT);

        SESSION = Session.getInstance(smtpProps, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
    }

    private MailSender() {
    }

    private static String getConfig(Properties props, String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return props.getProperty(key);
    }

    public static void send(String toEmail, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = new MimeMessage(SESSION);
        try {
            message.setFrom(new InternetAddress(USERNAME, FROM_NAME));
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Invalid from-name encoding", e);
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        try {
            message.setSubject(MimeUtility.encodeText(subject, "UTF-8", null));
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Invalid subject encoding", e);
        }
        message.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(message);
    }
}
