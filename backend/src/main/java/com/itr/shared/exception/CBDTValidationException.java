package com.itr.shared.exception;

/**
 * CBDTValidationException — thrown when ITD schema validation fails.
 */
public class CBDTValidationException extends RuntimeException {
    public CBDTValidationException(String message) {
        super(message);
    }
}
