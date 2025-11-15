package com.email.scheduler.email_scheduler_server.newsletter.consumer;

import static com.email.scheduler.email_scheduler_server.newsletter.config.RabbitMQConfig.QUEUE_NAME;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Message;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Message.MessageStatus;
import com.email.scheduler.email_scheduler_server.newsletter.domain.NewsletterMessage;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Subscriber;
import com.email.scheduler.email_scheduler_server.newsletter.repository.MessageRepository;
import com.email.scheduler.email_scheduler_server.newsletter.repository.SubscriberRepository;
import com.email.scheduler.email_scheduler_server.newsletter.service.EmailService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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

    @RabbitListener(queues = QUEUE_NAME)
    @Transactional
    public void receiveMessage(NewsletterMessage message) {
        long startTime = System.currentTimeMillis();
        log.info("[Consumer] 📨 뉴스레터 발송 시작 - 파일: {}", message.getFileName());

        String fileName = message.getFileName();
        String content = message.getContent();

        List<Subscriber> subscribers = subscriberRepository.findAllByActiveTrue();
        log.info("[Consumer] 👥 총 구독자 수: {}", subscribers.size());

        long queryTime = System.currentTimeMillis();
        log.info("[Consumer] ⏱️ 구독자 조회 시간: {}ms", queryTime - startTime);

        // 성공과 실패 카운터
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failCount = new AtomicInteger(0);

        // 병렬 처리로 동시 발송
        subscribers.parallelStream()
                .forEach(subscriber -> {
                    boolean success = processEmail(subscriber, content, fileName);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                });

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long emailTime = endTime - queryTime;

        log.info("[Consumer] ✅ 발송 완료 - 총 시간: {}ms, 이메일 발송 시간: {}ms", totalTime, emailTime);
        double avgTime = (double) emailTime / subscribers.size();
        log.info("[Consumer] 📈 평균 발송 속도: {}ms/구독자", String.format("%.2f", avgTime));
        log.info("[Consumer] 🔮 성공: {} FAIL: {} (성공률: {}%)",
                successCount.get(),
                failCount.get(),
                String.format("%.1f", (double) successCount.get() / subscribers.size() * 100));
    }

    private boolean processEmail(Subscriber subscriber, String content, String fileName) {
        try {
            boolean success = emailService.sendEmail(subscriber.getEmail(), content);
            saveMessageLog(subscriber, content, fileName, success);
            return success;
        } catch (Exception e) {
            log.error("[Consumer] Failed to send email to {}", subscriber.getEmail(), e);
            saveMessageLog(subscriber, content, fileName, false);
            return false;
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
