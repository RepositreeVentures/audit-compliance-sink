package io.repositree.audit.service;

import io.repositree.audit.dto.AuditQueryRequest;
import io.repositree.audit.dto.AuditQueryResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditQueryService {

    private final AthenaClient athena;
    private final String database;
    private final String outputBucket;

    public AuditQueryService(AthenaClient athena,
                             org.springframework.core.env.Environment env) {
        this.athena = athena;
        this.database = env.getProperty("sink.athena.database", "audit_compliance");
        this.outputBucket = env.getProperty("sink.athena.output-bucket", "s3://audit-athena-results/");
    }

    public AuditQueryResponse query(AuditQueryRequest request) {
        String sql = buildSql(request);
        String executionId = startQuery(sql);
        List<Row> rows = waitAndFetch(executionId);
        return toResponse(rows, executionId);
    }

    private String buildSql(AuditQueryRequest req) {
        List<String> conditions = new ArrayList<>();
        if (req.tenantId() != null) conditions.add("tenant_id = '" + req.tenantId().replace("'", "''") + "'");
        if (req.eventType() != null) conditions.add("event_type = '" + req.eventType().replace("'", "''") + "'");
        if (req.actorId() != null) conditions.add("actor_id = '" + req.actorId().replace("'", "''") + "'");
        if (req.from() != null) conditions.add("occurred_at >= TIMESTAMP '" + req.from() + "'");
        if (req.to() != null) conditions.add("occurred_at <= TIMESTAMP '" + req.to() + "'");

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        int limit = req.limit() > 0 ? Math.min(req.limit(), 10000) : 1000;
        return "SELECT event_id, event_type, tenant_id, actor_id, occurred_at, source_service, hash " +
               "FROM human_audit" + where + " ORDER BY occurred_at DESC LIMIT " + limit;
    }

    private String startQuery(String sql) {
        return athena.startQueryExecution(
                StartQueryExecutionRequest.builder()
                        .queryString(sql)
                        .queryExecutionContext(QueryExecutionContext.builder().database(database).build())
                        .resultConfiguration(ResultConfiguration.builder().outputLocation(outputBucket).build())
                        .build()
        ).queryExecutionId();
    }

    private List<Row> waitAndFetch(String executionId) {
        while (true) {
            var status = athena.getQueryExecution(
                    GetQueryExecutionRequest.builder().queryExecutionId(executionId).build()
            ).queryExecution().status();
            switch (status.state()) {
                case SUCCEEDED -> {
                    return athena.getQueryResults(
                            GetQueryResultsRequest.builder().queryExecutionId(executionId).build()
                    ).resultSet().rows();
                }
                case FAILED, CANCELLED ->
                        throw new RuntimeException("Athena query failed: " + status.stateChangeReason());
                default -> { try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
            }
        }
    }

    private AuditQueryResponse toResponse(List<Row> rows, String queryId) {
        if (rows.size() <= 1) return new AuditQueryResponse(List.of(), queryId);
        List<String> headers = rows.get(0).data().stream().map(Datum::varCharValue).toList();
        List<Map<String, String>> results = rows.subList(1, rows.size()).stream().map(row -> {
            var data = row.data();
            Map<String, String> record = new java.util.LinkedHashMap<>();
            for (int i = 0; i < headers.size() && i < data.size(); i++) {
                record.put(headers.get(i), data.get(i).varCharValue());
            }
            return record;
        }).collect(Collectors.toList());
        return new AuditQueryResponse(results, queryId);
    }
}
