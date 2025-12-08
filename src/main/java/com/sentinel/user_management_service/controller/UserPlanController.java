package com.sentinel.user_management_service.controller;

import com.sentinel.user_management_service.dto.response.UserPlanDTO;
import com.sentinel.user_management_service.service.UserPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/user-plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
public class UserPlanController {

    private final UserPlanService userPlanService;

    /**
     * Get current user plan details
     * GET /api/user-plans/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserPlanDTO> getMyPlan(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {

        // In a real scenario with SecurityContext, we would get principal.
        // For now, trusting the Gateway passed header or using a fallback for dev.
        // BUT strict security would require parsing the JWT here or using
        // SecurityContextHolder if JWT filter is active.

        if (userIdStr == null) {
            // If header is missing (e.g. direct call without gateway),
            // we could try SecurityContext if implemented, or return 400.
            // keeping it simple for MVP:
            return ResponseEntity.badRequest().build();
        }

        UUID userId = UUID.fromString(userIdStr);
        log.info("Fetching plan for user: {}", userId);

        return ResponseEntity.ok(userPlanService.getUserPlan(userId));
    }
}
