package io.repositree.audit.dto;

import java.util.List;
import java.util.Map;

public record DataExportResponse(
        String subjectId,
        String tenantId,
        List<Map<String, String>> records,
        String exportedAt
) {}
