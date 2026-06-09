package io.repositree.audit.service;

import io.repositree.audit.dto.AgentRunSearchRequest;
import io.repositree.audit.dto.AgentRunSummary;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentTraceService {

    private final AthenaClient athena;
    private final String database;
    private final String outputBucket;

    public AgentTraceService(AthenaClient athena,
                             org.springframework.core.env.Environment env) {
        this.athena = athena;
        this.database = env.getProperty("sink.athena.database", "audit_compliance");
        this.outputBucket = env.getProperty("sink.athena.output-bucket", "s3://audit-athena-results/");
    }

    public Map<String, Object> getRunTrace(String runId) {
        String sql = "SELECT * FROM agent_traces WHERE run_id = '" + runId.replace("'", "''") + "' LIMIT 1";
        List<Row> rows = waitAndFetch(startQuery(sql));
        if (rows.size() < 2) throw new io.repositree.audit.exception.NotFoundException("Run not found: " + runId);
        return rowToMap(rows.get(0), rows.get(1));
    }

    public List<AgentRunSummary> searchRuns(AgentRunSearchRequest req) {
        List<String> conditions = new ArrayList<>();
        if (req.tenantId() != null) conditions.add("tenant_id = '" + req.tenantId().replace("'", "''") + "'");
        if (req.agentId() != null) conditions.add("agent_id = '" + req.agentId().replace("'", "''") + "'");
        if (req.action() != null) conditions.add("action = '" + req.action().replace("'", "''") + "'");
        if (req.from() != null) conditions.add("started_at >= TIMESTAMP '" + req.from() + "'");
        if (req.to() != null) conditions.add("started_at <= TIMESTAMP '" + req.to() + "'");

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT run_id, agent_id, tenant_id, action, status, started_at, finished_at " +
                     "FROM agent_traces" + where + " ORDER BY started_at DESC LIMIT 500";

        List<Row> rows = waitAndFetch(startQuery(sql));
        if (rows.size() <= 1) return List.of();
        List<String> headers = rows.get(0).data().stream().map(Datum::varCharValue).toList();
        return rows.subList(1, rows.size()).stream()
                .map(row -> {
                    var m = rowToMap(rows.get(0), row);
                    return new AgentRunSummary(
                            (String) m.get("run_id"), (String) m.get("agent_id"),
                            (String) m.get("tenant_id"), (String) m.get("action"),
                            (String) m.get("status"), (String) m.get("started_at"),
                            (String) m.get("finished_at")
                    );
                })
                .collect(Collectors.toList());
    }

    private String startQuery(String sql) {
        return athena.startQueryExecution(
                StartQueryExecutionRequest.builder().queryString(sql)
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
                case FAILED, CANCELLED -> throw new RuntimeException("Athena: " + status.stateChangeReason());
                default -> { try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
            }
        }
    }

    private Map<String, Object> rowToMap(Row header, Row data) {
        var headers = header.data().stream().map(Datum::varCharValue).toList();
        var values = data.data();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (int i = 0; i < headers.size() && i < values.size(); i++) {
            result.put(headers.get(i), values.get(i).varCharValue());
        }
        return result;
    }
}
