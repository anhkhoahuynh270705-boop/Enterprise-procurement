package com.example.shopping.workflow.camunda.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowReviewEventPublisher {

    public static final String TOPIC_WORKFLOW_REVIEW_EVENTS = "workflow-review-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publish(WorkflowReviewEvent event) {
        kafkaTemplate.send(TOPIC_WORKFLOW_REVIEW_EVENTS, event.entityId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Không thể gửi event kết quả duyệt {} {} vào Kafka",
                                event.entityType(), event.referenceCode(), error);
                        return;
                    }
                    log.info("Đã gửi event kết quả duyệt {} {} vào Kafka topic {}",
                            event.entityType(), event.referenceCode(), TOPIC_WORKFLOW_REVIEW_EVENTS);
                });
    }
}
