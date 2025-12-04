package com.sentinel.user_management_service.events.listeners;

import com.sentinel.user_management_service.enums.ProjectRole;
import com.sentinel.user_management_service.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectCreatedListener {

    private final ProjectMemberService projectMemberService;

    @RabbitListener(queues = "user_mgmt.project.created.queue")
    public void handleProjectCreated(Map<String, Object> event) {
        try {
            log.info("Received project.created event: {}", event);

            String eventType = (String) event.get("eventType");
            
            if (!"project.created".equals(eventType)) {
                log.warn("Unexpected event type: {}", eventType);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            
            UUID projectId = UUID.fromString((String) data.get("projectId"));
            UUID tenantId = UUID.fromString((String) data.get("tenantId"));
            UUID ownerId = UUID.fromString((String) data.get("userId"));

            log.info("Adding owner {} as PROJECT_ADMIN to project {}", ownerId, projectId);

            projectMemberService.addMember(projectId, ownerId, tenantId, ProjectRole.PROJECT_ADMIN, null);

            log.info("Owner successfully added as PROJECT_ADMIN");

        } catch (Exception e) {
            log.error("Error processing project.created event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
