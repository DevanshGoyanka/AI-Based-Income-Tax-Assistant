package com.itr.domain.ruleengine;

public enum RuleLifecycleState {
    SUPPORTED,   // Fully verified, safe for real filings
    DRAFT,       // Scaffolded for future AY, values not yet verified
    ARCHIVED     // Past AY, kept for historical snapshot replay only
}
