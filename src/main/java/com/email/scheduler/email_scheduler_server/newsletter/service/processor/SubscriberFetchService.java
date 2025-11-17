package com.email.scheduler.email_scheduler_server.newsletter.service.processor;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Subscriber;
import com.email.scheduler.email_scheduler_server.newsletter.repository.SubscriberRepository;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriberFetchService {

    private final SubscriberRepository repository;

    private static final int PAGE_SIZE = 10_000;

    public long countActiveSubscribers() {
        return repository.countByActiveTrue();
    }

    public void processEachPage(Consumer<List<Subscriber>> handler) {
        int page = 0;

        while (true) {
            Page<Subscriber> p = repository.findAllByActiveTrue(
                    PageRequest.of(page, PAGE_SIZE)
            );

            if (p.isEmpty()) {
                break;
            }

            handler.accept(p.getContent());

            if (!p.hasNext()) {
                break;
            }

            page += 1;
        }
    }
}
