package com.itr.infrastructure.email;

/**
 * EmailProvider — interface for sending emails.
 */
public interface EmailProvider {

    /**
     * Send an email with optional attachments.
     *
     * @param to          Recipient email
     * @param subject     Email subject
     * @param body        HTML or plain text body
     * @param attachments Array of file paths or URLs to attach
     * @return true if sent successfully
     */
    boolean send(String to, String subject, String body, String... attachments);
}
