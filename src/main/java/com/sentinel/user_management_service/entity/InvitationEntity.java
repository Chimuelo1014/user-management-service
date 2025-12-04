package com.sentinel.user_management_service.entity;

import com.sentinel.user_management_service.enums.InvitationStatus;
import com.sentinel.user_management_service.enums.InvitationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitations", indexes = {
    @Index(name = "idx_invitations_email", columnList = "email"),
    @Index(name = "idx_invitations_token", columnList = "token"),
    @Index(name = "idx_invitations_status", columnList = "status"),
    @Index(name = "idx_invitations_resource_id", columnList = "resource_id"),
    @Index(name = "idx_invitations_type", columnList = "type"),
    @Index(name = "idx_invitations_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationType type;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "resource_name", length = 255)
    private String resourceName;

    @Column(nullable = false, length = 20)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "inviter_email", length = 255)
    private String inviterEmail;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING && !isExpired();
    }

    public boolean isAccepted() {
        return status == InvitationStatus.ACCEPTED;
    }

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    public void revoke() {
        this.status = InvitationStatus.REVOKED;
    }

    public void markExpired() {
        this.status = InvitationStatus.EXPIRED;
    }
}
