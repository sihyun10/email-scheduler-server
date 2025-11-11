package com.email.scheduler.email_scheduler_server.newsletter.repository;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Subscriber;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    List<Subscriber> findAllByActiveTrue();
}
