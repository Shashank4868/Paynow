package com.personal.notification.consumer;

import com.personal.notification.dto.NotificationEvent;
import com.personal.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaConsumer {

    private final NotificationService notificationService;

    public NotificationKafkaConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${notification.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(NotificationEvent event) {
        notificationService.process(event);
    }
}