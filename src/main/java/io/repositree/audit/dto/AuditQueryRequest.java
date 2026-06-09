package io.repositree.audit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AuditQueryRequest(
        String tenantId,
        String eventType,
        String actorId,
        String from,
        String to,
        @Min(1) @Max(10000) int limit
) {}
