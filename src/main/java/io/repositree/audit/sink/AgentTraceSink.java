package io.repositree.audit.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class AgentTraceSink {

    private static final Logger log = LoggerFactory.getLogger(AgentTraceSink.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    private final S3Client s3;
    private final ObjectMapper objectMapper;
    private final String bucket;

    public AgentTraceSink(S3Client s3, ObjectMapper objectMapper,
                          @Value("${sink.agent-trace.bucket}") String bucket) {
        this.s3 = s3;
        this.objectMapper = objectMapper;
        this.bucket = bucket;
    }

    public void write(EnrichedAgentTrace trace) {
        try {
            String date = DATE_FMT.format(trace.startedAt());
            String key = String.format("date=%s/agent_id=%s/%s.json",
                    date, sanitize(trace.agentId()), trace.runId());

            byte[] body = objectMapper.writeValueAsBytes(trace);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/json")
                    .build();

            s3.putObject(request, RequestBody.fromBytes(body));
            log.debug("Agent trace written: bucket={} key={}", bucket, key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to agent-trace S3 sink: " + e.getMessage(), e);
        }
    }

    private String sanitize(String value) {
        return value == null ? "UNKNOWN" : value.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
