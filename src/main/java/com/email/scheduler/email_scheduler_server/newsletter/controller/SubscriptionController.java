package com.email.scheduler.email_scheduler_server.newsletter.controller;

import com.email.scheduler.email_scheduler_server.newsletter.dto.SubscriptionRequest;
import com.email.scheduler.email_scheduler_server.newsletter.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<String> subscribe(@Valid @RequestBody SubscriptionRequest request) {
        log.info("구독 요청 수신: {}", request.getEmail());
        subscriptionService.subscribe(request.getEmail());
        return ResponseEntity.ok("구독이 성공적으로 처리되었습니다.");
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@Valid @RequestBody SubscriptionRequest request) {
        log.info("구독 취소 요청 수신: {}", request.getEmail());
        subscriptionService.unsubscribe(request.getEmail());
        return ResponseEntity.ok("구독이 성공적으로 취소되었습니다.");
    }
}
