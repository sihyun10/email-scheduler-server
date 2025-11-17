package com.email.scheduler.email_scheduler_server.newsletter.service.processor;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Message;
import com.email.scheduler.email_scheduler_server.newsletter.domain.NewsletterMessage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterProcessor {

    private final SubscriberFetchService fetchService;
    private final EmailBatchService batchService;
    private final MessageLogService logService;

    public void process(NewsletterMessage messages) {
        long start = System.currentTimeMillis();
        long total = fetchService.countActiveSubscribers();

        log.info("[Processor] 👥 총 구독자: {}", total);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        fetchService.processEachPage(subscribers -> {
            List<Message> logs = batchService.sendBatch(subscribers, messages, success, fail);
            logService.saveLogsInBatch(logs);
        });

        long duration = System.currentTimeMillis() - start;
        printSummary(total, success, fail, duration);
    }

    private void printSummary(long total, AtomicInteger success, AtomicInteger fail, long time) {
        log.info("[Processor] ✅ 전체 완료 - {}ms", time);
        log.info("[Processor] 📈 평균 처리속도: {}ms/구독자",
                String.format("%.2f", (double) time / total));
        log.info("[Processor] 🔮 성공: {} FAIL: {} (성공률: {}%)",
                success.get(), fail.get(),
                String.format("%.1f", success.get() * 100.0 / total));
    }
}
