package com.email.scheduler.email_scheduler_server.example.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "emailFanoutExchange";
    public static final String QUEUE_NAME_1 = "emailQueue1";
    public static final String QUEUE_NAME_2 = "emailQueue2";
    public static final String QUEUE_NAME_3 = "emailQueue3";

    @Bean
    public Queue emailQueue1() {
        return new Queue(QUEUE_NAME_1, true);
    }

    @Bean
    public Queue emailQueue2() {
        return new Queue(QUEUE_NAME_2, true);
    }

    @Bean
    public Queue emailQueue3() {
        return new Queue(QUEUE_NAME_3, true);
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding1(Queue emailQueue1, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(emailQueue1).to(fanoutExchange);
    }

    @Bean
    public Binding binding2(Queue emailQueue2, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(emailQueue2).to(fanoutExchange);
    }

    @Bean
    public Binding binding3(Queue emailQueue3, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(emailQueue3).to(fanoutExchange);
    }
}
