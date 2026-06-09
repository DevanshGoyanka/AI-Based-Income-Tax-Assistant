package com.itr.infrastructure.whatsapp;

/**
 * WhatsAppClient — interface for Meta WhatsApp Business Cloud API.
 */
public interface WhatsAppClient {

    /**
     * Send a text message to a WhatsApp number.
     *
     * @param phoneNumber Recipient phone number (with country code)
     * @param message     Message text
     * @return Message ID from Meta
     */
    String sendText(String phoneNumber, String message);

    /**
     * Send a template message (pre-approved by Meta).
     *
     * @param phoneNumber Recipient phone number
     * @param templateName Template name
     * @param parameters   Template variable values
     * @return Message ID from Meta
     */
    String sendTemplate(String phoneNumber, String templateName, String[] parameters);

    /**
     * Send a document (PDF, etc.) via WhatsApp.
     *
     * @param phoneNumber  Recipient phone number
     * @param documentUrl  Public URL or presigned S3 URL
     * @param documentName Display name
     * @return Message ID from Meta
     */
    String sendDocument(String phoneNumber, String documentUrl, String documentName);
}
