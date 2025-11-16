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

        // 1. 병렬 처리 중 로그 객체를 안전하게 모으기 위한 동기화된 리스트 생성
        List<Message> messageLogs = Collections.synchronizedList(new ArrayList<>(subscribers.size()));

        // 병렬 처리로 동시 발송
        subscribers.parallelStream()
                .forEach(subscriber -> {
                    boolean success = false;
                    try {
                        // 2. 이메일 발송
                        success = emailService.sendEmail(subscriber.getEmail(), content);
                    } catch (Exception e) {
                        log.error("[Consumer] Failed to send email to {}", subscriber.getEmail(), e);
                    }

                    // 3. DB 저장을 위해 로그 객체만 생성하여 리스트에 추가
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

        // 4. 병렬 스트림이 완료된 후, 트랜잭션 내에서 한 번에 벌크 삽입
        long batchStartTime = System.currentTimeMillis();
        messageRepository.saveAll(messageLogs);
        long batchEndTime = System.currentTimeMillis();
        log.info("[Consumer] DB Batch Insert 시간: {}ms", batchEndTime - batchStartTime);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long finalEmailTime = endTime - queryTime; // 이메일 발송 + 배치 시간

        log.info("[Consumer] ✅ 발송 완료 - 총 시간: {}ms, 이메일 발송+배치 시간: {}ms", totalTime, finalEmailTime);
        double avgTime = (double) finalEmailTime / subscribers.size();
        log.info("[Consumer] 📈 평균 발송 속도: {}ms/구독자", String.format("%.2f", avgTime));
        log.info("[Consumer] 🔮 성공: {} FAIL: {} (성공률: {}%)",
                successCount.get(),
                failCount.get(),
                String.format("%.1f", (double) successCount.get() / subscribers.size() * 100));
    }
}
