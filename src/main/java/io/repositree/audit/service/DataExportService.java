package io.repositree.audit.service;

import io.repositree.audit.dto.DataExportResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataExportService {

    private final AthenaClient athena;
    private final String database;
    private final String outputBucket;

    public DataExportService(AthenaClient athena,
                             org.springframework.core.env.Environment env) {
        this.athena = athena;
        this.database = env.getProperty("sink.athena.database", "audit_compliance");
        this.outputBucket = env.getProperty("sink.athena.output-bucket", "s3://audit-athena-results/");
    }

    public DataExportResponse exportForSubject(String subjectId, String tenantId) {
        // Query all events where actor_id = subjectId for DPDP/GDPR subject access requests
        String sql = String.format(
                "SELECT event_id, event_type, occurred_at, source_service, redacted_payload " +
                "FROM human_audit WHERE actor_id = '%s' AND tenant_id = '%s' ORDER BY occurred_at",
                subjectId.replace("'", "''"), tenantId.replace("'", "''"));

        List<Row> rows = waitAndFetch(startQuery(sql));
        if (rows.size() <= 1) return new DataExportResponse(subjectId, tenantId, List.of(), Instant.now().toString());

        List<String> headers = rows.get(0).data().stream().map(Datum::varCharValue).toList();
        List<Map<String, String>> records = rows.subList(1, rows.size()).stream().map(row -> {
            var data = row.data();
            Map<String, String> record = new java.util.LinkedHashMap<>();
            for (int i = 0; i < headers.size() && i < data.size(); i++) {
                record.put(headers.get(i), data.get(i).varCharValue());
            }
            return record;
        }).collect(Collectors.toList());

        return new DataExportResponse(subjectId, tenantId, records, Instant.now().toString());
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
}
