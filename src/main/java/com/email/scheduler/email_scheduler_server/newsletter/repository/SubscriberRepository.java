package com.email.scheduler.email_scheduler_server.newsletter.repository;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Subscriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    Page<Subscriber> findAllByActiveTrue(Pageable pageable);
}
