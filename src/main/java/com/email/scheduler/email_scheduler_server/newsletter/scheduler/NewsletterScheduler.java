package com.email.scheduler.email_scheduler_server.newsletter.scheduler;

import com.email.scheduler.email_scheduler_server.newsletter.service.NewsletterPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsletterScheduler {

    private final NewsletterPublisher publisher;
    private boolean allNewslettersSent = false;

    @Scheduled(fixedRate = 10000)
    public void sendDailyNewsletter() {
        if (allNewslettersSent) {
            return;
        }

        log.info("⏰ [Scheduler] 뉴스레터 발송 시작");
        boolean hasMore = publisher.publishNewsletter();

        if (!hasMore) {
            allNewslettersSent = true;
            log.info("[Scheduler] 모든 뉴스레터 발송 완료. 스케줄러 중지");
        } else {
            log.info("[Scheduler] 뉴스레터 발송 완료");
        }
    }
}
