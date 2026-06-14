package com.seal.seal_backend.common.exception;

/** Throw on RBAC / scope violations (e.g. judge scoring an unassigned round). Maps to HTTP 403. */
public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) { super(message); }
}
