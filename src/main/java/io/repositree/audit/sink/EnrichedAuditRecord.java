package io.repositree.audit.sink;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EnrichedAuditRecord(
        String eventId,
        String eventType,
        String tenantId,
        String actorId,
        String actorType,
        Instant occurredAt,
        String redactedPayload,
        String correlationId,
        String sourceService,
        String prevHash,
        String hash,
        Instant ingestedAt
) {}
