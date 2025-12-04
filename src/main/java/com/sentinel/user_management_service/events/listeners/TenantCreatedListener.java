package com.sentinel.user_management_service.events.listeners;

import com.sentinel.user_management_service.enums.TenantRole;
import com.sentinel.user_management_service.service.TenantMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCreatedListener {

    private final TenantMemberService tenantMemberService;

    @RabbitListener(queues = "user_mgmt.tenant.created.queue")
    public void handleTenantCreated(Map<String, Object> event) {
        try {
            log.info("Received tenant.created event: {}", event);

            String eventType = (String) event.get("eventType");
            
            if (!"tenant.created".equals(eventType)) {
                log.warn("Unexpected event type: {}", eventType);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            
            UUID tenantId = UUID.fromString((String) data.get("tenantId"));
            UUID ownerId = UUID.fromString((String) data.get("userId"));

            log.info("Adding owner {} as TENANT_ADMIN to tenant {}", ownerId, tenantId);

            tenantMemberService.addMember(tenantId, ownerId, TenantRole.TENANT_ADMIN, null);

            log.info("Owner successfully added as TENANT_ADMIN");

        } catch (Exception e) {
            log.error("Error processing tenant.created event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
