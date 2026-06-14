package com.seal.seal_backend.common.exception;

/**
 * Throw when a domain/business rule is violated (BR-* in the SRS).
 * Maps to HTTP 409 Conflict. This is where most service-layer validation lands.
 */
public class BusinessRuleException extends RuntimeException {
    private final String ruleCode;
    public BusinessRuleException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }
    public String getRuleCode() { return ruleCode; }
}
