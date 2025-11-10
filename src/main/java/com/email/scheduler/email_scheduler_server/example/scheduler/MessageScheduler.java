package com.email.scheduler.email_scheduler_server.example.scheduler;

import com.email.scheduler.email_scheduler_server.example.producer.EmailProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageScheduler {

    private final EmailProducer emailProducer;

    @Scheduled(fixedRate = 10000)
    public void sendEmailMessage() {
        String message = "[스케줄러] 이메일 발송 요청 📨";
        log.info("[스케줄러] 메시지 생성: {}", message);
        emailProducer.sendMessage(message);
    }
}
