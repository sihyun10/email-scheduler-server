package com.email.scheduler.email_scheduler_server.newsletter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    public boolean sendEmail(String email, String content) {
        try {
            // 실제 이메일 발송 로직 (예: JavaMailSender 사용)
            // 여기서는 성공을 가정하고 로그 남기기
            log.info("[Email Service] Newsletter has been sent to {}", email);
            return true;
        } catch (Exception e) {
            log.error("[Email Service] Failed to send newsletter to {}", email, e);
            return false;
        }
    }
}
