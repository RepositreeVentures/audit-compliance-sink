package io.repositree.audit.dto;

public record AgentRunSearchRequest(
        String tenantId,
        String agentId,
        String action,
        String from,
        String to,
        int limit
) {}
