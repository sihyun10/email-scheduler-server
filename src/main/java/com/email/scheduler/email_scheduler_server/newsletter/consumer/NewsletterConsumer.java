package com.email.scheduler.email_scheduler_server.newsletter.consumer;

import static com.email.scheduler.email_scheduler_server.newsletter.config.RabbitMQConfig.QUEUE_NAME;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Message;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Message.MessageStatus;
import com.email.scheduler.email_scheduler_server.newsletter.domain.NewsletterMessage;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Subscriber;
import com.email.scheduler.email_scheduler_server.newsletter.repository.SubscriberRepository;
import com.email.scheduler.email_scheduler_server.newsletter.service.EmailService;
import com.email.scheduler.email_scheduler_server.newsletter.service.MessageLogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsletterConsumer {

    private final SubscriberRepository subscriberRepository;
    private final MessageLogService messageLogService;
    private final EmailService emailService;

    // 한 번에 처리할 구독자 수 (10,000명)
    private static final int PAGE_SIZE = 10_000;

    @RabbitListener(queues = QUEUE_NAME)
    public void receiveMessage(NewsletterMessage message) {
        long startTime = System.currentTimeMillis();

        logStart(message);
        long totalSubscribers = logSubscriberCount();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        processSubscribers(message, successCount, failCount);

        logFinish(startTime, totalSubscribers, successCount, failCount);
    }

    private void logStart(NewsletterMessage message) {
        log.info("[Consumer] 📨 뉴스레터 발송 시작 - 파일: {}", message.getFileName());
    }

    private long logSubscriberCount() {
        long count = subscriberRepository.count();
        log.info("[Consumer] 👥 총 구독자 수: {}", count);
        return count;
    }

    private void logFinish(long startTime, long total, AtomicInteger success, AtomicInteger fail) {
        long totalTime = System.currentTimeMillis() - startTime;

        log.info("[Consumer] ✅ 발송 완료 - 총 시간: {}ms", totalTime);
        log.info("[Consumer] 📈 평균 처리 속도: {}ms/구독자",
                String.format("%.2f", (double) totalTime / total));
        log.info("[Consumer] 🔮 성공: {} FAIL: {} (성공률: {}%)",
                success.get(),
                fail.get(),
                String.format("%.1f", success.get() * 100.0 / total));
    }

    // 전체 Paging 처리
    private void processSubscribers(
            NewsletterMessage message,
            AtomicInteger successCount,
            AtomicInteger failCount
    ) {
        int pageNumber = 0;

        while (true) {
            Page<Subscriber> subscriberPage = loadSubscriberPage(pageNumber);

            if (subscriberPage.getContent().isEmpty()) {
                break;
            }

            List<Message> logs = processPage(subscriberPage.getContent(), message, successCount, failCount);

            messageLogService.saveLogsInBatch(logs);

            if (!subscriberPage.hasNext()) {
                break;
            }
            pageNumber += 1;
        }
    }

    // 페이지 조회
    private Page<Subscriber> loadSubscriberPage(int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
        return subscriberRepository.findAllByActiveTrue(pageable);
    }

    // 페이지 처리 (병렬 이메일 발송 + 로그 생성)
    private List<Message> processPage(
            List<Subscriber> subscribers,
            NewsletterMessage message,
            AtomicInteger successCount,
            AtomicInteger failCount
    ) {
        List<Message> logs = Collections.synchronizedList(new ArrayList<>(subscribers.size()));

        subscribers.parallelStream()
                .forEach(subscriber -> {
                    boolean success = sendEmailSafely(subscriber, message.getContent());

                    logs.add(buildLog(subscriber, message, success));

                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                });

        return logs;
    }

    // 이메일 발송 (예외 안전 처리)
    private boolean sendEmailSafely(Subscriber subscriber, String content) {
        try {
            return emailService.sendEmail(subscriber.getEmail(), content);
        } catch (Exception e) {
            log.error("[Consumer] Failed to send email to {}", subscriber.getEmail(), e);
            return false;
        }
    }

    // Message 로그 객체 생성
    private Message buildLog(Subscriber subscriber, NewsletterMessage msg, boolean success) {
        return Message.builder()
                .subscriber(subscriber)
                .fileName(msg.getFileName())
                .content(msg.getContent())
                .sendAt(LocalDateTime.now())
                .status(success ? MessageStatus.SUCCESS : MessageStatus.FAILURE)
                .build();
    }
}
