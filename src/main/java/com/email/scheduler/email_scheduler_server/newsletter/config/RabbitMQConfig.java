package com.email.scheduler.email_scheduler_server.newsletter.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "newsletter.exchange";
    public static final String QUEUE_NAME = "newsletter.queue";

    @Bean
    public FanoutExchange newsletterExchange() {
        return new FanoutExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue newsletterQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding(Queue newsletterQueue, FanoutExchange newsletterExchange) {
        return BindingBuilder.bind(newsletterQueue).to(newsletterExchange);
    }
}
