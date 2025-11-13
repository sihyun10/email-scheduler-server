package com.email.scheduler.email_scheduler_server.newsletter.consumer;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Message;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Message.MessageStatus;
import com.email.scheduler.email_scheduler_server.newsletter.domain.NewsletterMessage;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Subscriber;
import com.email.scheduler.email_scheduler_server.newsletter.repository.MessageRepository;
import com.email.scheduler.email_scheduler_server.newsletter.repository.SubscriberRepository;
import com.email.scheduler.email_scheduler_server.newsletter.service.EmailService;
import java.time.LocalDateTime;
import java.util.List;
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
    public void receiveMessage(NewsletterMessage message) {
        log.info("[Consumer] Received newsletter content. Preparing to send to subscribers.");

        String fileName = message.getFileName();
        String content = message.getContent();

        List<Subscriber> subscribers = subscriberRepository.findAllByActiveTrue();

        // 병렬 처리로 동시 발송
        subscribers.parallelStream()
                .forEach(subscriber -> processEmail(subscriber, content, fileName));

        log.info("[Consumer] Finished sending newsletters to all {} subscribers.", subscribers.size());
    }

    private void processEmail(Subscriber subscriber, String content, String fileName) {
        try {
            boolean success = emailService.sendEmail(subscriber.getEmail(), content);
            saveMessageLog(subscriber, content, fileName, success);
            log.info("[Consumer] Email sent to {}: {}", subscriber.getEmail(), success ? "SUCCESS" : "FAILURE");
        } catch (Exception e) {
            log.error("[Consumer] Failed to send email to {}", subscriber.getEmail(), e);
            saveMessageLog(subscriber, content, fileName, false);
        }
    }

    private void saveMessageLog(Subscriber subscriber, String content, String fileName, boolean success) {
        Message message = Message.builder()
                .subscriber(subscriber)
                .content(content)
                .fileName(fileName)
                .sendAt(LocalDateTime.now())
                .status(success ? MessageStatus.SUCCESS : MessageStatus.FAILURE)
                .build();
        messageRepository.save(message);
    }
}
