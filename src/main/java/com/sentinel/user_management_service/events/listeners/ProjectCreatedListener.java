package com.sentinel.user_management_service.events.listeners;

import com.sentinel.user_management_service.enums.ProjectRole;
import com.sentinel.user_management_service.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Escucha eventos de project-service para asignar roles automáticamente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectCreatedListener {

    private final ProjectMemberService projectMemberService;

    /**
     * Consume: project.created (desde project-service)
     * Queue: user_mgmt.project.created.queue
     * 
     * Asigna el owner como PROJECT_ADMIN automáticamente.
     */
    @RabbitListener(queues = "user_mgmt.project.created.queue")
    @Transactional
    public void handleProjectCreated(Map<String, Object> event) {
        try {
            log.info("📥 Received event: project.created - {}", event);

            // Validar tipo de evento
            String eventType = (String) event.get("eventType");
            if (!"project.created".equals(eventType)) {
                log.warn("⚠️ Unexpected event type: {}", eventType);
                return;
            }

            // Extraer datos directamente del evento
            UUID projectId = UUID.fromString((String) event.get("projectId"));
            UUID tenantId = UUID.fromString((String) event.get("tenantId"));
            UUID ownerId = UUID.fromString((String) event.get("ownerId"));
            String projectName = (String) event.get("projectName");

            log.info("🔄 Adding owner {} as PROJECT_ADMIN to project {} ({})",
                    ownerId, projectId, projectName);

            projectMemberService.addMember(projectId, ownerId, tenantId, ProjectRole.PROJECT_ADMIN, null);

            log.info("✅ Owner successfully added as PROJECT_ADMIN to project: {}", projectId);

        } catch (Exception e) {
            log.error("❌ Error processing project.created event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

}