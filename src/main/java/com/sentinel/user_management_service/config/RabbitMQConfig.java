package com.sentinel.user_management_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.exchange.user-mgmt:user_mgmt_exchange}")
    private String userMgmtExchange;

    @Value("${spring.rabbitmq.exchange.auth:auth_exchange}")
    private String authExchange;

    // ========================================
    // EXCHANGES
    // ========================================

    @Bean
    public TopicExchange userMgmtExchange() {
        return new TopicExchange(userMgmtExchange, true, false);
    }

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(authExchange, true, false);
    }

    // ========================================
    // QUEUES (Para consumir)
    // ========================================

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder
                .durable("user_mgmt.user.registered.queue")
                .build();
    }

    // ========================================
    // BINDINGS (Para consumir)
    // ========================================

    @Bean
    public Binding userRegisteredBinding() {
        return BindingBuilder
                .bind(userRegisteredQueue())
                .to(authExchange())
                .with("auth.user.registered");
    }

    // ========================================
    // Billing Integration
    // ========================================

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange("sentinel-billing-exchange", true, false);
    }

    @Bean
    public Queue billingSubscriptionQueue() {
        return QueueBuilder.durable("user_mgmt.billing.subscription.queue").build();
    }

    @Bean
    public Binding billingSubscriptionBinding() {
        return BindingBuilder
                .bind(billingSubscriptionQueue())
                .to(billingExchange())
                .with("billing.subscription.created");
    }

    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        // Crucial: Create a DefaultClassMapper and set TrustedPackages to "*" to avoid
        // security restrictions on deserialization
        org.springframework.amqp.support.converter.DefaultClassMapper classMapper = new org.springframework.amqp.support.converter.DefaultClassMapper();
        classMapper.setTrustedPackages("*"); // Trust everything (safe for internal microservices)
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
