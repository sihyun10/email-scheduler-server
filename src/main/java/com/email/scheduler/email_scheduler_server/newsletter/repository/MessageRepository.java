package com.email.scheduler.email_scheduler_server.newsletter.repository;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
