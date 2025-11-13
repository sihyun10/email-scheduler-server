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

    // @Scheduled(fixedRate = 10000) // 테스트용 스케줄러 : 개발/테스트 시 빠르게 확인하기 위해 사용 (10초 간격)
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    public void sendDailyNewsletter() {
        boolean hasNewsletter = publisher.publishNewsletter();

        if (hasNewsletter) {
            log.info("📡 [Scheduler] 뉴스레터를 Exchange에 전송했습니다");
        }
        // 보낼 뉴스레터가 없으면 조용히 대기
    }
}
