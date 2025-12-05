package com.sentinel.user_management_service.client;

import com.sentinel.user_management_service.client.dto.TenantDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(
    name = "tenant-service",
    url = "${services.tenant.url}"
)
public interface TenantServiceClient {

    @CircuitBreaker(name = "tenantService", fallbackMethod = "getTenantFallback")
    @Retry(name = "tenantService")
    @GetMapping("/api/tenants/internal/{id}")
    TenantDTO getTenant(@PathVariable UUID id);

    default TenantDTO getTenantFallback(UUID id, Exception ex) {
        throw new RuntimeException("Tenant service unavailable: " + ex.getMessage());
    }
}
