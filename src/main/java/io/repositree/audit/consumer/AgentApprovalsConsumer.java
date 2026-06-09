package io.repositree.audit.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.repositree.audit.domain.ApprovalRecord;
import io.repositree.audit.service.ApprovalSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class AgentApprovalsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AgentApprovalsConsumer.class);

    private final ApprovalSinkService sinkService;
    private final ObjectMapper objectMapper;

    public AgentApprovalsConsumer(ApprovalSinkService sinkService, ObjectMapper objectMapper) {
        this.sinkService = sinkService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "agent.approvals", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            ApprovalRecord record = objectMapper.readValue(message, ApprovalRecord.class);
            sinkService.process(record);
            log.debug("Processed approval: offset={} approvalId={}", offset, record.approvalId());
        } catch (Exception e) {
            log.error("Failed to process approval: offset={} error={}", offset, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
