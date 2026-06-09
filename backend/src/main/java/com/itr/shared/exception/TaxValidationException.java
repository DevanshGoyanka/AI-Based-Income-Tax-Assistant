package com.itr.shared.exception;

/**
 * TaxValidationException — thrown on business rule violation.
 */
public class TaxValidationException extends RuntimeException {
    public TaxValidationException(String message) {
        super(message);
    }
    public TaxValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
