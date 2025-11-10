package com.email.scheduler.email_scheduler_server.example.repository;

import com.email.scheduler.email_scheduler_server.example.domain.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
}
