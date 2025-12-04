package com.sentinel.user_management_service.events.listeners;

import com.sentinel.user_management_service.enums.TenantRole;
import com.sentinel.user_management_service.service.TenantMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Escucha eventos de tenant-service para asignar roles automáticamente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCreatedListener {

    private final TenantMemberService tenantMemberService;

    /**
     * Consume: tenant.created (desde tenant-service)
     * Queue: user_mgmt.tenant.created.queue
     * 
     * Asigna el owner como TENANT_ADMIN automáticamente.
     */
    @RabbitListener(queues = "user_mgmt.tenant.created.queue")
    @Transactional
    public void handleTenantCreated(Map<String, Object> event) {
        try {
            log.info("📥 Received event: tenant.created - {}", event);

            // Validar estructura del evento
            String eventType = (String) event.get("eventType");
            
            if (!"tenant.created".equals(eventType)) {
                log.warn("⚠️ Unexpected event type: {}", eventType);
                return;
            }

            // Extraer datos del evento
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            
            UUID tenantId = UUID.fromString((String) data.get("tenantId"));
            UUID ownerId = UUID.fromString((String) data.get("userId"));
            String tenantName = (String) data.get("name");

            log.info("🔄 Adding owner {} as TENANT_ADMIN to tenant {} ({})", 
                ownerId, tenantId, tenantName);

            // Agregar owner como TENANT_ADMIN
            tenantMemberService.addMember(tenantId, ownerId, TenantRole.TENANT_ADMIN, null);

            log.info("✅ Owner successfully added as TENANT_ADMIN to tenant: {}", tenantId);

        } catch (Exception e) {
            log.error("❌ Error processing tenant.created event: {}", e.getMessage(), e);
            // Re-lanzar para que RabbitMQ reintente o envíe a DLQ
            throw new RuntimeException("Event processing failed", e);
        }
    }
}