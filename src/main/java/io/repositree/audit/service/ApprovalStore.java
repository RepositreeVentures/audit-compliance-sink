package io.repositree.audit.service;

import io.repositree.audit.domain.ApprovalRecord;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

import java.util.List;
import java.util.Map;

@Component
public class ApprovalStore {

    private final AthenaClient athena;
    private final String database;
    private final String outputBucket;

    public ApprovalStore(AthenaClient athena,
                         org.springframework.core.env.Environment env) {
        this.athena = athena;
        this.database = env.getProperty("sink.athena.database", "audit_compliance");
        this.outputBucket = env.getProperty("sink.athena.output-bucket", "s3://audit-athena-results/");
    }

    public ApprovalRecord find(String approvalId) {
        String query = String.format(
                "SELECT approval_id, run_id, approver_id, tenant_id, decision, " +
                "signed_payload, signature, signer_public_key, decided_at, prev_hash, hash " +
                "FROM agent_approvals WHERE approval_id = '%s' LIMIT 1",
                approvalId.replace("'", "''"));

        String executionId = startQuery(query);
        List<Row> rows = waitAndFetch(executionId);

        if (rows.size() < 2) return null; // row 0 is header
        Row row = rows.get(1);
        List<Datum> data = row.data();

        return new ApprovalRecord(
                get(data, 0), get(data, 1), get(data, 2), get(data, 3),
                get(data, 4), get(data, 5), get(data, 6), get(data, 7),
                get(data, 8) != null ? java.time.Instant.parse(get(data, 8)) : null,
                get(data, 9), get(data, 10)
        );
    }

    private String startQuery(String sql) {
        StartQueryExecutionResponse resp = athena.startQueryExecution(
                StartQueryExecutionRequest.builder()
                        .queryString(sql)
                        .queryExecutionContext(QueryExecutionContext.builder()
                                .database(database).build())
                        .resultConfiguration(ResultConfiguration.builder()
                                .outputLocation(outputBucket).build())
                        .build());
        return resp.queryExecutionId();
    }

    private List<Row> waitAndFetch(String executionId) {
        while (true) {
            QueryExecutionStatus status = athena.getQueryExecution(
                    GetQueryExecutionRequest.builder().queryExecutionId(executionId).build()
            ).queryExecution().status();

            switch (status.state()) {
                case SUCCEEDED -> {
                    return athena.getQueryResults(
                            GetQueryResultsRequest.builder().queryExecutionId(executionId).build()
                    ).resultSet().rows();
                }
                case FAILED, CANCELLED -> throw new RuntimeException(
                        "Athena query failed: " + status.stateChangeReason());
                default -> {
                    try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }
        }
    }

    private String get(List<Datum> data, int i) {
        return (i < data.size()) ? data.get(i).varCharValue() : null;
    }
}
