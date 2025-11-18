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
            return reactivateIfInactive(optionalSubscriber.get());
        } else {
            return createNewSubscriber(email);
        }
    }

    @Transactional
    public void unsubscribe(String email) {
        subscriberRepository.findByEmail(email).ifPresent(subscriber -> {
            if (subscriber.isActive()) {
                subscriber.deactivate();
                subscriberRepository.save(subscriber);
            }
        });
    }

    // 이미 존재하는 구독자 처리
    // 비활성 상태 ➔ 활성화하고 저장, 활성 상태 ➔ 그대로 반환
    private Subscriber reactivateIfInactive(Subscriber subscriber) {
        if (!subscriber.isActive()) {
            subscriber.activate();
            return subscriberRepository.save(subscriber);
        }

        return subscriber;
    }

    // 신규 구독자 생성하고, 동시성 충돌 시 재조회하여 처리
    private Subscriber createNewSubscriber(String email) {
        try {
            Subscriber newSubscriber = new Subscriber(email);
            return subscriberRepository.save(newSubscriber);
        } catch (DataIntegrityViolationException e) {
            return subscriberRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException(
                            "동시성 충돌 이후 이메일로 구독자를 찾지 못했습니다. email= " + email));
        }
    }
}
