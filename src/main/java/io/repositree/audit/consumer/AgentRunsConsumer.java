package io.repositree.audit.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.repositree.audit.domain.AgentTrace;
import io.repositree.audit.service.AgentTraceSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class AgentRunsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AgentRunsConsumer.class);

    private final AgentTraceSinkService sinkService;
    private final ObjectMapper objectMapper;

    public AgentRunsConsumer(AgentTraceSinkService sinkService, ObjectMapper objectMapper) {
        this.sinkService = sinkService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"agent.runs", "agent.tool_calls", "agent.guardrail.violations", "agent.cost.events"},
            groupId = "${spring.kafka.consumer.group-id}")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            AgentTrace trace = objectMapper.readValue(message, AgentTrace.class);
            sinkService.process(trace);
            log.debug("Processed agent trace: topic={} offset={} runId={}", topic, offset, trace.runId());
        } catch (Exception e) {
            log.error("Failed to process agent trace: topic={} offset={} error={}", topic, offset, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
