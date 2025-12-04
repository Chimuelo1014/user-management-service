package com.sentinel.user_management_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQListenerConfig {

    @Bean
    public TopicExchange tenantExchange() {
        return new TopicExchange("tenant-exchange", true, false);
    }

    @Bean
    public TopicExchange projectExchange() {
        return new TopicExchange("project-exchange", true, false);
    }

    @Bean
    public Queue tenantCreatedQueue() {
        return new Queue("user_mgmt.tenant.created.queue", true);
    }

    @Bean
    public Queue projectCreatedQueue() {
        return new Queue("user_mgmt.project.created.queue", true);
    }

    @Bean
    public Binding tenantCreatedBinding(Queue tenantCreatedQueue, TopicExchange tenantExchange) {
        return BindingBuilder
                .bind(tenantCreatedQueue)
                .to(tenantExchange)
                .with("tenant.created");
    }

    @Bean
    public Binding projectCreatedBinding(Queue projectCreatedQueue, TopicExchange projectExchange) {
        return BindingBuilder
                .bind(projectCreatedQueue)
                .to(projectExchange)
                .with("project.created");
    }
}
