package com.sentinel.user_management_service.repository;

import com.sentinel.user_management_service.entity.InvitationEntity;
import com.sentinel.user_management_service.enums.InvitationStatus;
import com.sentinel.user_management_service.enums.InvitationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<InvitationEntity, UUID> {

    Optional<InvitationEntity> findByToken(String token);

    List<InvitationEntity> findByEmail(String email);

    List<InvitationEntity> findByEmailAndStatus(String email, InvitationStatus status);

    List<InvitationEntity> findByResourceIdAndType(UUID resourceId, InvitationType type);

    List<InvitationEntity> findByResourceIdAndTypeAndStatus(UUID resourceId, InvitationType type, InvitationStatus status);

    Optional<InvitationEntity> findByEmailAndResourceIdAndTypeAndStatus(String email, UUID resourceId, InvitationType type, InvitationStatus status);

    boolean existsByEmailAndResourceIdAndTypeAndStatus(String email, UUID resourceId, InvitationType type, InvitationStatus status);

    List<InvitationEntity> findByStatus(InvitationStatus status);

    @Query("SELECT i FROM InvitationEntity i WHERE i.status = :status AND i.expiresAt < :now")
    List<InvitationEntity> findExpiredInvitations(@Param("status") InvitationStatus status, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE InvitationEntity i SET i.status = :newStatus WHERE i.status = :oldStatus AND i.expiresAt < :now")
    int markExpiredInvitations(@Param("oldStatus") InvitationStatus oldStatus, @Param("newStatus") InvitationStatus newStatus, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM InvitationEntity i WHERE i.status = :status AND i.createdAt < :cutoffDate")
    void deleteOldInvitations(@Param("status") InvitationStatus status, @Param("cutoffDate") LocalDateTime cutoffDate);
}
