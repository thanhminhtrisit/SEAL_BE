package com.seal.seal_backend.common.exception;

/** Throw when an entity is not found. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
    public ResourceNotFoundException(String entity, Object id) {
        super(entity + " not found: " + id);
    }
}
