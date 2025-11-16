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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsletterConsumer {

    private final SubscriberRepository subscriberRepository;
    private final MessageRepository messageRepository;
    private final EmailService emailService;

    // 한 번에 처리할 구독자 수 (10,000명)
    private static final int PAGE_SIZE = 10_000;

    @RabbitListener(queues = QUEUE_NAME)
    @Transactional
    public void receiveMessage(NewsletterMessage message) {
        long startTime = System.currentTimeMillis();
        log.info("[Consumer] 📨 뉴스레터 발송 시작 - 파일: {}", message.getFileName());

        String fileName = message.getFileName();
        String content = message.getContent();

        long totalSubscribers = subscriberRepository.count();
        log.info("[Consumer] 👥 총 구독자 수: {}", totalSubscribers);

        // 성공과 실패 카운터
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failCount = new AtomicInteger(0);

        int pageNumber = 0;
        boolean hasNext = true;

        long queryTime = System.currentTimeMillis();

        // 1. Paging Loop 시작 : 구독자를 PAGE_SIZE 단위로 조회/처리
        while (hasNext) {
            Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
            Page<Subscriber> subscriberPage = subscriberRepository.findAllByActiveTrue(pageable);

            List<Subscriber> currentSubscribers = subscriberPage.getContent();

            if (currentSubscribers.isEmpty()) {
                break;
            }

            // 현재 페이지 로그 객체를 담을 동기화된 리스트
            List<Message> messageLogs = Collections.synchronizedList(new ArrayList<>(currentSubscribers.size()));

            // 2. 페이지별 병렬 처리
            currentSubscribers.parallelStream()
                    .forEach(subscriber -> {
                        boolean success = false;
                        try {
                            success = emailService.sendEmail(subscriber.getEmail(), content);
                        } catch (Exception e) {
                            log.error("[Consumer] Failed to send email to {}", subscriber.getEmail(), e);
                        }

                        // 로그 객체 생성
                        Message log = Message.builder()
                                .subscriber(subscriber)
                                .content(content)
                                .fileName(fileName)
                                .sendAt(LocalDateTime.now())
                                .status(success ? MessageStatus.SUCCESS : MessageStatus.FAILURE)
                                .build();

                        messageLogs.add(log);

                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    });

            // 3. 페이지별 Batch Insert
            messageRepository.saveAll(messageLogs);

            // 다음 페이지로 이동
            pageNumber += 1;
            hasNext = subscriberPage.hasNext();
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        log.info("[Consumer] ⏱️ 구독자 조회 시작 시간: {}ms", queryTime - startTime);
        log.info("[Consumer] ✅ 발송 완료 - 총 시간: {}ms", totalTime);
        double avgTime = (double) totalTime / totalSubscribers;
        log.info("[Consumer] 📈 평균 처리 속도: {}ms/구독자", String.format("%.2f", avgTime));
        log.info("[Consumer] 🔮 성공: {} FAIL: {} (성공률: {}%)",
                successCount.get(),
                failCount.get(),
                String.format("%.1f", (double) successCount.get() / totalSubscribers * 100));
    }
}
