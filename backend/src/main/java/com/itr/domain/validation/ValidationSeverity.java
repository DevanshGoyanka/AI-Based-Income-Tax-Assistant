package com.itr.domain.validation;

/**
 * ValidationSeverity - BLOCKING stops pipeline, WARNING surfaces but doesn't block.
 * Document 1 §6.2
 */
public enum ValidationSeverity {
    BLOCKING,
    WARNING
}
