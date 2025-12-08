package com.sentinel.user_management_service.events;

import com.sentinel.user_management_service.entity.InvitationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserManagementEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.exchange.user-mgmt:user_mgmt_exchange}")
    private String exchange;

    @Value("${spring.rabbitmq.routing-key.user-invited:user.invited}")
    private String userInvitedKey;

    @Value("${spring.rabbitmq.routing-key.invitation-accepted:user.invitation.accepted}")
    private String invitationAcceptedKey;

    /**
     * Publica: user.invited
     * Consumidor: notification-service (enviar email)
     */
    public void publishUserInvited(InvitationEntity invitation) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("eventType", "user.invited");
            event.put("timestamp", LocalDateTime.now());
            
            Map<String, Object> data = new HashMap<>();
            data.put("invitationId", invitation.getId().toString());
            data.put("email", invitation.getEmail());
            data.put("resourceId", invitation.getResourceId().toString());
            data.put("resourceName", invitation.getResourceName());
            data.put("resourceType", invitation.getType().name());
            data.put("inviterEmail", invitation.getInviterEmail());
            data.put("role", invitation.getRole());
            data.put("invitationToken", invitation.getToken());
            data.put("invitationUrl", "http://localhost:3000/invitations/accept?token=" + invitation.getToken());
            data.put("expiresAt", invitation.getExpiresAt().toString());
            
            event.put("data", data);

            rabbitTemplate.convertAndSend(exchange, userInvitedKey, event);
            log.info("✅ Event published: user.invited for {}", invitation.getEmail());

        } catch (Exception e) {
            log.error("❌ Failed to publish user.invited event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publica: user.invitation.accepted
     * Consumidor: analytics, audit-log
     */
    public void publishInvitationAccepted(InvitationEntity invitation, UUID userId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("eventType", "user.invitation.accepted");
            event.put("timestamp", LocalDateTime.now());
            
            Map<String, Object> data = new HashMap<>();
            data.put("invitationId", invitation.getId().toString());
            data.put("userId", userId.toString());
            data.put("email", invitation.getEmail());
            data.put("resourceId", invitation.getResourceId().toString());
            data.put("resourceType", invitation.getType().name());
            data.put("role", invitation.getRole());
            
            event.put("data", data);

            rabbitTemplate.convertAndSend(exchange, invitationAcceptedKey, event);
            log.info("✅ Event published: user.invitation.accepted for user {}", userId);

        } catch (Exception e) {
            log.error("❌ Failed to publish invitation.accepted event: {}", e.getMessage(), e);
        }
    }
}

