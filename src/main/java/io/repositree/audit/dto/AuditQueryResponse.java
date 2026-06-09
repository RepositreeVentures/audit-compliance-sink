package io.repositree.audit.dto;

import java.util.List;
import java.util.Map;

public record AuditQueryResponse(List<Map<String, String>> results, String queryExecutionId) {}
