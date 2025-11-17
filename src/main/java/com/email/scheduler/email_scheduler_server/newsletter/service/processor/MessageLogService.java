package com.email.scheduler.email_scheduler_server.newsletter.service.processor;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Message;
import com.email.scheduler.email_scheduler_server.newsletter.repository.MessageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageLogService {

    private final MessageRepository messageRepository;

    /**
     * 이 메서드에만 @Transactional을 적용하여 트랜잭션 범위를 로그 저장으로 한정함
     */
    @Transactional
    public void saveLogsInBatch(List<Message> messageLogs) {
        if (!messageLogs.isEmpty()) {
            messageRepository.saveAll(messageLogs);
        }
    }
}
