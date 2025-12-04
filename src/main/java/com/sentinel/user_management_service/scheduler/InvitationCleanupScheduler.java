package com.sentinel.user_management_service.scheduler;

import com.sentinel.user_management_service.service.InvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationCleanupScheduler {

    private final InvitationService invitationService;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredInvitations() {
        log.info("Starting cleanup of expired invitations");
        
        try {
            invitationService.cleanupExpiredInvitations();
            log.info("Invitation cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error during invitation cleanup: {}", e.getMessage(), e);
        }
    }
}
