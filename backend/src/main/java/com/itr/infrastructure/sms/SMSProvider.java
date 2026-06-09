package com.itr.infrastructure.sms;

/**
 * SMSProvider — interface for sending SMS messages.
 */
public interface SMSProvider {
    /**
     * Send an SMS message.
     *
     * @param mobile  Recipient mobile number
     * @param message Message text (will be truncated per provider limits)
     * @return true if sent successfully
     */
    boolean send(String mobile, String message);
}
