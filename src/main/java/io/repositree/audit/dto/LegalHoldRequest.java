package io.repositree.audit.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record LegalHoldRequest(
        @NotNull String tenantId,
        @NotNull String reason,
        Instant dateFrom,
        Instant dateTo
) {}
