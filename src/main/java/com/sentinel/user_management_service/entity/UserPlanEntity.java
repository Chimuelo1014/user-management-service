package com.sentinel.user_management_service.entity;

import com.sentinel.user_management_service.enums.UserPlan;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPlanEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserPlan plan = UserPlan.FREE;

    @Column(nullable = false)
    @Builder.Default
    private int maxTenants = 3;

    @Column(nullable = false)
    @Builder.Default
    private int maxProjectsPerTenant = 5;

    @Column(nullable = false)
    @Builder.Default
    private int maxUsersPerTenant = 10;

    @Column(nullable = false)
    @Builder.Default
    private int maxScansPerMonth = 100;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean canCreateTenant(int currentCount) {
        return currentCount < maxTenants;
    }

    public boolean canAddUserToTenant(int currentUsers) {
        return currentUsers < maxUsersPerTenant;
    }

    public boolean canCreateProject(int currentProjects) {
        return currentProjects < maxProjectsPerTenant;
    }

    public void upgradePlan(UserPlan newPlan) {
        this.plan = newPlan;
        this.maxTenants = newPlan.getMaxTenants();
        this.maxProjectsPerTenant = newPlan.getMaxProjectsPerTenant();
        this.maxUsersPerTenant = newPlan.getMaxUsersPerTenant();
        this.maxScansPerMonth = newPlan.getMaxScansPerMonth();
    }
}