package com.sentinel.user_management_service.client;

import com.sentinel.user_management_service.client.dto.ProjectDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(
    name = "project-service",
    url = "${services.project.url}"
)
public interface ProjectServiceClient {

    @CircuitBreaker(name = "projectService", fallbackMethod = "getProjectFallback")
    @Retry(name = "projectService")
    @GetMapping("/api/projects/internal/{id}")
    ProjectDTO getProject(@PathVariable UUID id);

    default ProjectDTO getProjectFallback(UUID id, Exception ex) {
        throw new RuntimeException("Project service unavailable: " + ex.getMessage());
    }
}