package com.email.scheduler.email_scheduler_server.newsletter.consumer;

import static com.email.scheduler.email_scheduler_server.newsletter.config.RabbitMQConfig.QUEUE_NAME;

import com.email.scheduler.email_scheduler_server.newsletter.domain.NewsletterMessage;
import com.email.scheduler.email_scheduler_server.newsletter.service.processor.NewsletterProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsletterConsumer {

    private final NewsletterProcessor processor;

    @RabbitListener(queues = QUEUE_NAME)
    public void receiveMessage(NewsletterMessage message) {
        log.info("[Consumer] 📨 메시지 수신 - 파일: {}", message.getFileName());
        processor.process(message);
    }
}
