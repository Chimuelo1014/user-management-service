package com.sentinel.user_management_service.entity;

import com.sentinel.user_management_service.enums.ProjectRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}),
    indexes = {
        @Index(name = "idx_project_members_project_id", columnList = "project_id"),
        @Index(name = "idx_project_members_user_id", columnList = "user_id"),
        @Index(name = "idx_project_members_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_project_members_role", columnList = "role")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectRole role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "added_by")
    private UUID addedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isAdmin() {
        return role == ProjectRole.PROJECT_ADMIN;
    }

    public boolean canManageMembers() {
        return role == ProjectRole.PROJECT_ADMIN;
    }

    public boolean canCreateScans() {
        return role == ProjectRole.PROJECT_ADMIN || role == ProjectRole.PROJECT_MEMBER;
    }

    public boolean canViewResults() {
        return true; // All members can view
    }
}