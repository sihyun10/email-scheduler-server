package com.email.scheduler.email_scheduler_server.example.producer;

import static com.email.scheduler.email_scheduler_server.example.config.RabbitMQConfig.EXCHANGE_NAME;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        log.info("[Producer] 메시지 전송 중: {}", message);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, "", message);
    }
}
