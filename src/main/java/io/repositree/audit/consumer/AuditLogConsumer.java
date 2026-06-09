package io.repositree.audit.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.repositree.audit.domain.AuditEvent;
import io.repositree.audit.service.AuditSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class AuditLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogConsumer.class);

    private final AuditSinkService sinkService;
    private final ObjectMapper objectMapper;

    public AuditLogConsumer(AuditSinkService sinkService, ObjectMapper objectMapper) {
        this.sinkService = sinkService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"audit.log", "payment.events"}, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            AuditEvent event = objectMapper.readValue(message, AuditEvent.class);
            sinkService.process(event);
            log.debug("Processed audit event: topic={} partition={} offset={} eventId={}", topic, partition, offset, event.eventId());
        } catch (Exception e) {
            log.error("Failed to process audit event: topic={} offset={} error={}", topic, offset, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
