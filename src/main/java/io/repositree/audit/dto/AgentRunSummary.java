package io.repositree.audit.dto;

public record AgentRunSummary(
        String runId,
        String agentId,
        String tenantId,
        String action,
        String status,
        String startedAt,
        String finishedAt
) {}
