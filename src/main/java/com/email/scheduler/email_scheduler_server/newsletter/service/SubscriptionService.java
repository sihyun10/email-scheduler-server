package com.email.scheduler.email_scheduler_server.newsletter.service;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Subscriber;
import com.email.scheduler.email_scheduler_server.newsletter.repository.SubscriberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriberRepository subscriberRepository;

    @Transactional
    public Subscriber subscribe(String email) {
        Optional<Subscriber> optionalSubscriber = subscriberRepository.findByEmail(email);

        if (optionalSubscriber.isPresent()) {
            Subscriber subscriber = optionalSubscriber.get();

            if (!subscriber.isActive()) {
                subscriber.activate();
                return subscriberRepository.save(subscriber);
            }
            
            return subscriber;
        } else {
            try {
                Subscriber newSubscriber = new Subscriber(email);
                return subscriberRepository.save(newSubscriber);
            } catch (DataIntegrityViolationException e) {
                // 동시성 이슈로 다른 트랜잭션이 먼저 커밋한 경우, 다시 한번 이메일 조회
                return subscriberRepository.findByEmail(email)
                        .orElseThrow(() -> new IllegalStateException(
                                "동시성 충돌 이후 이메일로 구독자를 찾지 못했습니다. email= " + email));
            }
        }
    }
}
