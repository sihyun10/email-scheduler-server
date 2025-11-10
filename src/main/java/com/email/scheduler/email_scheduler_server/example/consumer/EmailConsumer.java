package com.email.scheduler.email_scheduler_server.example.consumer;

import static com.email.scheduler.email_scheduler_server.example.config.RabbitMQConfig.QUEUE_NAME;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailConsumer {

    @RabbitListener(queues = QUEUE_NAME)
    public void receiveMessage(String message) {
        log.info("[Consumer] 메시지 수신 완료: {}", message);
    }
}
