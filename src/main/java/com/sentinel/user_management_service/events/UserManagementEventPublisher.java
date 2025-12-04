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

    @Value("${user_mgmt.events.exchange}")
    private String exchange;

    @Value("${user_mgmt.events.user-invited-routing-key}")
    private String userInvitedKey;

    @Value("${user_mgmt.events.invitation-accepted-routing-key}")
    private String invitationAcceptedKey;

    @Value("${user_mgmt.events.access-revoked-routing-key}")
    private String accessRevokedKey;

    public void publishUserInvited(InvitationEntity invitation) {
        log.info("Publishing user.invited event for: {}", invitation.getEmail());

        Map<String, Object> event = buildBaseEvent("user.invited");
        
        Map<String, Object> data = new HashMap<>();
        data.put("invitationId", invitation.getId().toString());
        data.put("email", invitation.getEmail());
        data.put("type", invitation.getType().name());
        data.put("resourceId", invitation.getResourceId().toString());
        data.put("resourceName", invitation.getResourceName());
        data.put("role", invitation.getRole());
        data.put("invitedBy", invitation.getInvitedBy().toString());
        data.put("expiresAt", invitation.getExpiresAt().toString());
        
        event.put("data", data);

        rabbitTemplate.convertAndSend(exchange, userInvitedKey, event);
    }

    public void publishInvitationAccepted(InvitationEntity invitation, UUID userId) {
        log.info("Publishing invitation.accepted event for: {}", invitation.getEmail());

        Map<String, Object> event = buildBaseEvent("user.invitation.accepted");
        
        Map<String, Object> data = new HashMap<>();
        data.put("invitationId", invitation.getId().toString());
        data.put("userId", userId.toString());
        data.put("email", invitation.getEmail());
        data.put("type", invitation.getType().name());
        data.put("resourceId", invitation.getResourceId().toString());
        data.put("role", invitation.getRole());
        
        event.put("data", data);

        rabbitTemplate.convertAndSend(exchange, invitationAcceptedKey, event);
    }

    public void publishAccessRevoked(UUID userId, UUID resourceId, String resourceType) {
        log.info("Publishing access.revoked event for user: {}", userId);

        Map<String, Object> event = buildBaseEvent("user.access.revoked");
        
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId.toString());
        data.put("resourceId", resourceId.toString());
        data.put("resourceType", resourceType);
        
        event.put("data", data);

        rabbitTemplate.convertAndSend(exchange, accessRevokedKey, event);
    }

    private Map<String, Object> buildBaseEvent(String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("eventId", UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("version", "1.0");
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "user-management-service");
        metadata.put("correlationId", UUID.randomUUID().toString());
        
        event.put("metadata", metadata);
        
        return event;
    }
}

