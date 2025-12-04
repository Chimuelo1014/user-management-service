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

    @Value("${user_mgmt.events.exchange}")
    private String userMgmtExchange;

    @Bean
    public TopicExchange userMgmtExchange() {
        return new TopicExchange(userMgmtExchange, true, false);
    }

    @Bean
    public Queue userInvitedQueue() {
        return new Queue("user_mgmt.user.invited.queue", true);
    }

    @Bean
    public Queue invitationAcceptedQueue() {
        return new Queue("user_mgmt.invitation.accepted.queue", true);
    }

    @Bean
    public Queue accessRevokedQueue() {
        return new Queue("user_mgmt.access.revoked.queue", true);
    }

    @Bean
    public Binding userInvitedBinding() {
        return BindingBuilder
                .bind(userInvitedQueue())
                .to(userMgmtExchange())
                .with("user.invited");
    }

    @Bean
    public Binding invitationAcceptedBinding() {
        return BindingBuilder
                .bind(invitationAcceptedQueue())
                .to(userMgmtExchange())
                .with("user.invitation.accepted");
    }

    @Bean
    public Binding accessRevokedBinding() {
        return BindingBuilder
                .bind(accessRevokedQueue())
                .to(userMgmtExchange())
                .with("user.access.revoked");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
