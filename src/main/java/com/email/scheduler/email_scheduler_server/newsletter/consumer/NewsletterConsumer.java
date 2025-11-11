package com.email.scheduler.email_scheduler_server.newsletter.consumer;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Message;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Message.MessageStatus;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Subscriber;
import com.email.scheduler.email_scheduler_server.newsletter.repository.MessageRepository;
import com.email.scheduler.email_scheduler_server.newsletter.repository.SubscriberRepository;
import com.email.scheduler.email_scheduler_server.newsletter.service.EmailService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsletterConsumer {

    private final SubscriberRepository subscriberRepository;
    private final MessageRepository messageRepository;
    private final EmailService emailService;

    @RabbitListener(queues = "newsletter.queue")
    @Transactional
    public void receiveMessage(String messageContent) {
        log.info("[Consumer] Received newsletter content. Preparing to send to subscribers.");

        subscriberRepository.findAllByActiveTrue()
                .forEach(subscriber -> {
                    boolean success = emailService.sendEmail(subscriber.getEmail(), messageContent);
                    saveMessageLog(subscriber, messageContent, success);
                });

        log.info("[Consumer] Finished sending newsletters to all subscribers.");
    }

    private void saveMessageLog(Subscriber subscriber, String content, boolean success) {
        Message message = Message.builder()
                .subscriber(subscriber)
                .content(content)
                .sendAt(LocalDateTime.now())
                .status(success ? MessageStatus.SUCCESS : MessageStatus.FAILURE)
                .build();
        messageRepository.save(message);
    }
}
