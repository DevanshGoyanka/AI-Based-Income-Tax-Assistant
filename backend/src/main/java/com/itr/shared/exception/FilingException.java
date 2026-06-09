package com.itr.shared.exception;

/**
 * FilingException — thrown on ITD portal errors (timeout, rejection, etc.).
 */
public class FilingException extends RuntimeException {
    public FilingException(String message) {
        super(message);
    }
    public FilingException(String message, Throwable cause) {
        super(message, cause);
    }
}
