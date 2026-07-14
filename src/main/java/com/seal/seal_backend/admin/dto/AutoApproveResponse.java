package com.seal.seal_backend.admin.dto;

/** Current state of the AUTO_APPROVE_ACCOUNTS toggle. ('enabled' avoids the record boolean is-prefix trap.) */
public record AutoApproveResponse(boolean enabled) {}
