package com.email.scheduler.email_scheduler_server.newsletter.service.processor;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Message;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Message.MessageStatus;
import com.email.scheduler.email_scheduler_server.newsletter.domain.NewsletterMessage;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Subscriber;
import com.email.scheduler.email_scheduler_server.newsletter.service.EmailService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailBatchService {

    private final EmailService emailService;

    public List<Message> sendBatch(
            List<Subscriber> subscribers,
            NewsletterMessage message,
            AtomicInteger success,
            AtomicInteger fail
    ) {
        List<Message> logs = new ArrayList<>(subscribers.size());

        for (Subscriber subscriber : subscribers) {
            boolean ok = sendSafely(subscriber.getEmail(), message.getContent());

            logs.add(buildLog(subscriber, message, ok));

            if (ok) {
                success.incrementAndGet();
            } else {
                fail.incrementAndGet();
            }
        }

        return logs;
    }

    private boolean sendSafely(String email, String content) {
        try {
            return emailService.sendEmail(email, content);
        } catch (Exception e) {
            log.error("[Sender] 이메일 전송 실패 - {}", email, e);
            return false;
        }
    }

    private Message buildLog(Subscriber subscriber, NewsletterMessage message, boolean ok) {
        return Message.builder()
                .subscriber(subscriber)
                .fileName(message.getFileName())
                .content(message.getContent())
                .sendAt(LocalDateTime.now())
                .status(ok ? MessageStatus.SUCCESS : MessageStatus.FAILURE)
                .build();
    }
}
