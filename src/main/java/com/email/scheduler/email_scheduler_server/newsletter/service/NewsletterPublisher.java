package com.email.scheduler.email_scheduler_server.newsletter.service;

import static com.email.scheduler.email_scheduler_server.newsletter.config.RabbitMQConfig.EXCHANGE_NAME;

import com.email.scheduler.email_scheduler_server.newsletter.domain.NewsletterMessage;
import com.email.scheduler.email_scheduler_server.newsletter.repository.MessageRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class
NewsletterPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MessageRepository messageRepository;

    public boolean publishNewsletter() {
        try {
            List<String> newsletterFiles = loadNewsletterFiles();
            String nextFileName = determineNextNewsletterFile(newsletterFiles);
            if (nextFileName == null) {
                log.info("모든 뉴스레터를 전송 완료했습니다. 스케줄러를 중지합니다.");
                return false;
            }

            var resource = new PathMatchingResourcePatternResolver()
                    .getResource("classpath:newsletters/" + nextFileName);
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            NewsletterMessage message = new NewsletterMessage(nextFileName, content);

            rabbitTemplate.convertAndSend(EXCHANGE_NAME, "", message);
            log.info("📨 [Publisher] Sent newsletter file: {}", nextFileName);
            return true;
        } catch (IOException e) {
            log.error("Failed to read newsletter content", e);
            throw new RuntimeException(e);
        }
    }

    private List<String> loadNewsletterFiles() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:newsletters/*.md");

        return Stream.of(resources)
                .map(Resource::getFilename)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(this::extractNumber))
                .collect(Collectors.toList());
    }

    private String determineNextNewsletterFile(List<String> newsletterFiles) {
        return messageRepository.findLastSentFileName()
                .map(lastSent -> {
                    int currentIndex = newsletterFiles.indexOf(lastSent);
                    int nextIndex = currentIndex + 1;
                    if (nextIndex >= newsletterFiles.size()) {
                        return null;
                    }
                    return newsletterFiles.get(nextIndex);
                })
                .orElse(newsletterFiles.getFirst()); // 첫 실행 시 첫 번째 뉴스레터 발송
    }

    private int extractNumber(String fileName) {
        try {
            String number = fileName.replaceAll("[^0-9]", "");
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
