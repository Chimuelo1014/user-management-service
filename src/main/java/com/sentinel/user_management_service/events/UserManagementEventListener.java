package com.sentinel.user_management_service.events;

import com.sentinel.user_management_service.service.UserPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserManagementEventListener {

    private final UserPlanService userPlanService;

    /**
     * Consume: auth.user.registered
     * Crea plan FREE automáticamente para nuevo usuario
     */
    @RabbitListener(queues = "user_mgmt.user.registered.queue")
    @Transactional
    public void handleUserRegistered(Map<String, Object> event) {
        try {
            log.info("📥 Received event: auth.user.registered");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");

            UUID userId = UUID.fromString((String) data.get("userId"));
            String email = (String) data.get("email");

            // ✅ Crear plan FREE por defecto
            userPlanService.createDefaultPlan(userId);
            log.info("✅ Default plan created for user: {} ({})", userId, email);

        } catch (Exception e) {
            log.error("❌ Failed to handle user.registered event: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Consume: billing.subscription.created
     * Actualiza el plan del usuario cuando se confirma el pago
     */
    @RabbitListener(queues = "user_mgmt.billing.subscription.queue")
    @Transactional
    public void handleSubscriptionCreated(Map<String, Object> event) {
        try {
            log.info("📥 Received event: billing.subscription.created");

            // Publisher sends flat JSON, so we read directly from event
            String userIdStr = (String) event.get("userId");
            String planId = (String) event.get("planId");

            if (userIdStr == null || planId == null) {
                log.warn("⚠️ Missing userId or planId in event data: {}", event);
                return;
            }

            UUID userId = UUID.fromString(userIdStr);
            // Map planId from DB (e.g., "STANDARD") to Enum (FREE, STANDARD, PRO,
            // ENTERPRISE)
            // Assuming planId matches enum names. If planId is UUID, we might need mapping
            // logic.
            // For this project, planId seems to be "STANDARD", "PRO" etc.
            try {
                com.sentinel.user_management_service.enums.UserPlan planEnum = com.sentinel.user_management_service.enums.UserPlan
                        .valueOf(planId.toUpperCase());

                userPlanService.upgradePlan(userId, planEnum);
                log.info("✅ Plan updated via event for user: {} to {}", userId, planEnum);
            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid Plan Enum: {}", planId);
            }

        } catch (Exception e) {
            log.error("❌ Failed to handle billing.subscription.created event: {}", e.getMessage(), e);
        }
    }
}