package io.repositree.audit.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InternalLagService {

    private final AdminClient adminClient;
    private final String consumerGroup;
    private final List<String> topics;

    public InternalLagService(AdminClient adminClient,
                              @Value("${spring.kafka.consumer.group-id:audit-compliance-sink}") String consumerGroup,
                              @Value("${sink.kafka.topics:audit.log,payment.events,agent.runs,agent.tool_calls,agent.approvals,agent.guardrail.violations,agent.cost.events}") String topicsStr) {
        this.adminClient = adminClient;
        this.consumerGroup = consumerGroup;
        this.topics = List.of(topicsStr.split(","));
    }

    public Map<String, Long> getLag() {
        try {
            ListConsumerGroupOffsetsResult groupOffsets = adminClient.listConsumerGroupOffsets(consumerGroup);
            Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> committed =
                    groupOffsets.partitionsToOffsetAndMetadata().get();

            Set<TopicPartition> partitions = committed.keySet().stream()
                    .filter(tp -> topics.contains(tp.topic()))
                    .collect(Collectors.toSet());

            Map<TopicPartition, OffsetSpec> latestQuery = partitions.stream()
                    .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

            Map<TopicPartition, org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo> latest =
                    adminClient.listOffsets(latestQuery).all().get();

            Map<String, Long> lagByTopic = new HashMap<>();
            for (String topic : topics) {
                long totalLag = partitions.stream()
                        .filter(tp -> tp.topic().equals(topic))
                        .mapToLong(tp -> {
                            long latestOffset = latest.getOrDefault(tp,
                                    new org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo(0, -1, java.util.Optional.empty())).offset();
                            long committedOffset = committed.getOrDefault(tp,
                                    new org.apache.kafka.clients.consumer.OffsetAndMetadata(0)).offset();
                            return Math.max(0, latestOffset - committedOffset);
                        }).sum();
                lagByTopic.put(topic, totalLag);
            }
            return lagByTopic;
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute Kafka lag: " + e.getMessage(), e);
        }
    }
}
