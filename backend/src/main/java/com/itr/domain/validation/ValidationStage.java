package com.itr.domain.validation;

/**
 * ValidationStage - validation execution phases.
 * Document 1 §6.2
 */
public enum ValidationStage {
    STRUCTURAL,      // Format/syntax checks
    IDENTITY,        // PAN/Aadhaar validation
    CONSISTENCY,     // Cross-field consistency
    RECONCILIATION,  // TDS/import reconciliation
    COMPLETENESS     // Mandatory fields present
}
