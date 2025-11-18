package com.email.scheduler.email_scheduler_server.newsletter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.email.scheduler.email_scheduler_server.newsletter.domain.Subscriber;
import com.email.scheduler.email_scheduler_server.newsletter.repository.SubscriberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriberRepository subscriberRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("신규 이메일로 구독 시, 구독자가 성공적으로 생성됨")
    void subscribe_NewEmail_ShouldCreateSubscriber() {
        // given
        String email = "new@test.com";
        Subscriber newSubscriber = Subscriber.builder().email(email).active(true).build();
        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(newSubscriber);

        // when
        Subscriber result = subscriptionService.subscribe(email);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.isActive()).isTrue();
        verify(subscriberRepository, times(1)).findByEmail(email);
        verify(subscriberRepository, times(1)).save(any(Subscriber.class));
    }

    @Test
    @DisplayName("이미 존재하는 활성 구독자로 구독 시, 기존 정보를 그대로 반환함")
    void subscribe_ExistingActiveSubscriber_ShouldReturnExistingSubscriber() {
        // given
        String email = "existing@test.com";
        Subscriber existingSubscriber = Subscriber.builder().email(email).active(true).build();
        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.of(existingSubscriber));

        // when
        Subscriber result = subscriptionService.subscribe(email);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.isActive()).isTrue();
        assertThat(result).isSameAs(existingSubscriber);
        verify(subscriberRepository, times(1)).findByEmail(email);
        verify(subscriberRepository, never()).save(any(Subscriber.class));
    }

    @Test
    @DisplayName("비활성 상태의 기존 구독자로 구독 시, 구독자를 활성 상태로 변경하고 저장함")
    void subscribe_InactiveSubscriber_ShouldReactivate() {
        // given
        String email = "inactive@test.com";
        Subscriber inactiveSubscriber = Subscriber.builder().email(email).active(false).build();

        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.of(inactiveSubscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Subscriber result = subscriptionService.subscribe(email);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isActive()).isTrue();
        verify(subscriberRepository, times(1)).findByEmail(email);
        verify(subscriberRepository, times(1)).save(inactiveSubscriber);
    }

    @Test
    @DisplayName("동시에 같은 이메일로 구독 요청 시, 멱등성을 보장하며 구독 처리")
    void subscribe_ConcurrentRequest_ShouldHandleRaceConditionAndEnsureIdempotency() {
        // given
        String email = "concurrent@test.com";
        Subscriber concurrentlySavedSubscriber = Subscriber.builder().email(email).active(true).build();

        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrentlySavedSubscriber));

        when(subscriberRepository.save(any(Subscriber.class))).thenThrow(
                new DataIntegrityViolationException("Simulated race condition"));

        // when
        Subscriber result = subscriptionService.subscribe(email);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isSameAs(concurrentlySavedSubscriber);
        verify(subscriberRepository, times(2)).findByEmail(email);
        verify(subscriberRepository, times(1)).save(any(Subscriber.class));
    }

    @Test
    @DisplayName("활성 구독자 구독 취소 시, 구독자를 비활성 상태로 변경")
    void unsubscribe_ActiveSubscriber_ShouldDeactivate() {
        // given
        String email = "active@test.com";
        Subscriber activeSubscriber = Subscriber.builder().email(email).active(true).build();
        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.of(activeSubscriber));

        // when
        subscriptionService.unsubscribe(email);

        // then
        assertThat(activeSubscriber.isActive()).isFalse();
        verify(subscriberRepository, times(1)).findByEmail(email);
        verify(subscriberRepository, times(1)).save(activeSubscriber);
    }

    @Test
    @DisplayName("비활성 구독자 구독 취소 시, 아무런 변경 없이 넘어감")
    void unsubscribe_InactiveSubscriber_ShouldDoNothing() {
        // given
        String email = "inactive@test.com";
        Subscriber inactiveSubscriber = Subscriber.builder().email(email).active(false).build();
        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.of(inactiveSubscriber));

        // when
        subscriptionService.unsubscribe(email);

        // then
        assertThat(inactiveSubscriber.isActive()).isFalse();
        verify(subscriberRepository, times(1)).findByEmail(email);
        verify(subscriberRepository, never()).save(any(Subscriber.class));
    }

    @Test
    @DisplayName("존재하지 않는 구독자 구독 취소 시, 아무런 변경 없이 넘어감")
    void unsubscribe_NonExistentSubscriber_ShouldDoNothing() {
        // given
        String email = "nonexistent@test.com";
        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when
        subscriptionService.unsubscribe(email);

        // then
        verify(subscriberRepository, times(1)).findByEmail(email);
        verify(subscriberRepository, never()).save(any(Subscriber.class));
    }
}
