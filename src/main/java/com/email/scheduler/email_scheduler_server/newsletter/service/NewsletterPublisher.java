package com.email.scheduler.email_scheduler_server.newsletter.service;

import static com.email.scheduler.email_scheduler_server.newsletter.config.RabbitMQConfig.EXCHANGE_NAME;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class
NewsletterPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishNewsletter() {
        try {
            String content = new String(
                    new ClassPathResource("newsletter.md").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            rabbitTemplate.convertAndSend(EXCHANGE_NAME, "", content);
            log.info("[Publisher] Newsletter content has been published.");

        } catch (IOException e) {
            log.error("Failed to read newsletter content", e);
            throw new RuntimeException(e);
        }
    }
}
