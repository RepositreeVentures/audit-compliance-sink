package io.repositree.audit.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditEvent(
        String eventId,
        String eventType,
        String tenantId,
        String actorId,
        String actorType,
        Instant occurredAt,
        JsonNode payload,
        String correlationId,
        String sourceService,
        String prevHash,
        String hash
) {}
