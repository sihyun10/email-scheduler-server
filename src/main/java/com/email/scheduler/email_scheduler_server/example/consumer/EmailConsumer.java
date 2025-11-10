package com.email.scheduler.email_scheduler_server.example.consumer;

import static com.email.scheduler.email_scheduler_server.example.config.RabbitMQConfig.QUEUE_NAME_1;
import static com.email.scheduler.email_scheduler_server.example.config.RabbitMQConfig.QUEUE_NAME_2;
import static com.email.scheduler.email_scheduler_server.example.config.RabbitMQConfig.QUEUE_NAME_3;

import com.email.scheduler.email_scheduler_server.example.domain.Subscriber;
import com.email.scheduler.email_scheduler_server.example.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private final SubscriberRepository subscriberRepository;

    @RabbitListener(queues = QUEUE_NAME_1)
    public void receiveMessage1(String message) {
        log.info("[Consumer-1] 📩 메시지 수신 완료: {}", message);
        subscriberRepository.save(new Subscriber("user1@email.com", message));
    }

    @RabbitListener(queues = QUEUE_NAME_2)
    public void receiveMessage2(String message) {
        log.info("[Consumer-2] 💌 메시지 수신 완료: {}", message);
        subscriberRepository.save(new Subscriber("user2@email.com", message));
    }

    @RabbitListener(queues = QUEUE_NAME_3)
    public void receiveMessage3(String message) {
        log.info("[Consumer-3] 📥 메시지 수신 완료: {}", message);
        subscriberRepository.save(new Subscriber("user3@email.com", message));
    }
}
