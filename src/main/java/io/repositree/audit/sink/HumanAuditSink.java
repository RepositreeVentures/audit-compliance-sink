package io.repositree.audit.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class HumanAuditSink {

    private static final Logger log = LoggerFactory.getLogger(HumanAuditSink.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    private final S3Client s3;
    private final ObjectMapper objectMapper;
    private final String bucket;

    public HumanAuditSink(S3Client s3, ObjectMapper objectMapper,
                          @Value("${sink.human-audit.bucket}") String bucket) {
        this.s3 = s3;
        this.objectMapper = objectMapper;
        this.bucket = bucket;
    }

    public void write(EnrichedAuditRecord record) {
        try {
            String date = DATE_FMT.format(record.occurredAt() != null ? record.occurredAt() : Instant.now());
            String key = String.format("date=%s/event_type=%s/%s.json",
                    date, sanitize(record.eventType()), record.eventId());

            byte[] body = objectMapper.writeValueAsBytes(record);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/json")
                    .build();

            s3.putObject(request, RequestBody.fromBytes(body));
            log.debug("Human audit record written: bucket={} key={}", bucket, key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to human-audit S3 sink: " + e.getMessage(), e);
        }
    }

    private String sanitize(String value) {
        return value == null ? "UNKNOWN" : value.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
