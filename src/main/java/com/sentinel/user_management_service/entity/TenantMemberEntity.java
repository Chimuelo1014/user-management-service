package com.sentinel.user_management_service.entity;

import com.sentinel.user_management_service.enums.TenantRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_members", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "user_id"}),
    indexes = {
        @Index(name = "idx_tenant_members_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tenant_members_user_id", columnList = "user_id"),
        @Index(name = "idx_tenant_members_role", columnList = "role")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMemberEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantRole role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isAdmin() {
        return role == TenantRole.TENANT_ADMIN;
    }

    public boolean canManageMembers() {
        return role == TenantRole.TENANT_ADMIN;
    }
}
