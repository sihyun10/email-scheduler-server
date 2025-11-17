package com.email.scheduler.email_scheduler_server.newsletter.service.processor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Message;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Message.MessageStatus;
import com.email.scheduler.email_scheduler_server.newsletter.domain.NewsletterMessage;
import com.email.scheduler.email_scheduler_server.newsletter.domain.Subscriber;
import com.email.scheduler.email_scheduler_server.newsletter.service.EmailService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailBatchServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailBatchService emailBatchService;

    private Subscriber createSubscriber(String email) {
        return Subscriber.builder()
                .id(1L)
                .email(email)
                .active(true)
                .messages(List.of())
                .build();
    }

    private NewsletterMessage createMessage() {
        return new NewsletterMessage("newsletter-01.txt", "Hello Subscribers!");
    }

    @Test
    @DisplayName("이메일 전송 성공 시 SUCCESS 로그 생성 & 성공 카운터 증가")
    void sendBatch_success() {
        // given
        Subscriber subscriber = createSubscriber("user@test.com");
        NewsletterMessage message = createMessage();

        when(emailService.sendEmail(anyString(), anyString())).thenReturn(true);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        // when
        List<Message> logs = emailBatchService.sendBatch(
                List.of(subscriber),
                message,
                success,
                fail
        );

        // then
        Assertions.assertThat(logs).hasSize(1);
        Message log = logs.get(0);

        Assertions.assertThat(log.getStatus()).isEqualTo(MessageStatus.SUCCESS);
        Assertions.assertThat(success.get()).isEqualTo(1);
        Assertions.assertThat(fail.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("이메일 전송 실패 시 FAILURE 로그 생성 & 실패 카운터 증가")
    void sendBatch_fail() {
        // given
        Subscriber subscriber = createSubscriber("user@test.com");
        NewsletterMessage message = createMessage();

        when(emailService.sendEmail(anyString(), anyString())).thenReturn(false);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        // when
        List<Message> logs = emailBatchService.sendBatch(
                List.of(subscriber),
                message,
                success,
                fail
        );

        // then
        Assertions.assertThat(logs).hasSize(1);
        Message log = logs.get(0);

        Assertions.assertThat(log.getStatus()).isEqualTo(MessageStatus.FAILURE);
        Assertions.assertThat(success.get()).isEqualTo(0);
        Assertions.assertThat(fail.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("이메일 전송 중 예외 발생 시 FAILURE 처리 & 실패 카운터 증가")
    void sendBatch_exception_handled() {
        // given
        Subscriber subscriber = createSubscriber("user@test.com");
        NewsletterMessage message = createMessage();

        when(emailService.sendEmail(anyString(), anyString()))
                .thenThrow(new RuntimeException("SMTP ERROR"));

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        // when
        List<Message> logs = emailBatchService.sendBatch(
                List.of(subscriber),
                message,
                success,
                fail
        );

        // then
        Assertions.assertThat(logs).hasSize(1);
        Message log = logs.get(0);

        Assertions.assertThat(log.getStatus()).isEqualTo(MessageStatus.FAILURE);
        Assertions.assertThat(success.get()).isEqualTo(0);
        Assertions.assertThat(fail.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("여러 구독자 처리 - 성공/실패 여부에 따라 각각 로그 상태 확인")
    void sendBatch_multipleSubscribers(boolean emailSuccess) {
        // given
        Subscriber s1 = createSubscriber("user1@test.com");
        Subscriber s2 = createSubscriber("user2@test.com");
        NewsletterMessage message = createMessage();

        when(emailService.sendEmail(anyString(), anyString()))
                .thenReturn(emailSuccess);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        // when
        List<Message> logs = emailBatchService.sendBatch(
                List.of(s1, s2),
                message,
                success,
                fail
        );

        // then
        Assertions.assertThat(logs).hasSize(2);

        if (emailSuccess) {
            Assertions.assertThat(success.get()).isEqualTo(2);
            Assertions.assertThat(fail.get()).isEqualTo(0);
            Assertions.assertThat(logs)
                    .extracting(Message::getStatus)
                    .containsOnly(MessageStatus.SUCCESS);
        } else {
            Assertions.assertThat(success.get()).isEqualTo(0);
            Assertions.assertThat(fail.get()).isEqualTo(2);
            Assertions.assertThat(logs)
                    .extracting(Message::getStatus)
                    .containsOnly(MessageStatus.FAILURE);
        }
    }
}
