package com.email.scheduler.email_scheduler_server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageScheduler {

    @Scheduled(fixedRate = 10000)
    public void testScheduler() {
        log.info("[테스트] 스케줄러가 10초마다 실행됩니다.");
    }
}
