package com.email.scheduler.email_scheduler_server.newsletter.repository;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Message;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m.fileName FROM Message m ORDER BY m.sendAt DESC LIMIT 1")
    Optional<String> findLastSentFileName();
}
