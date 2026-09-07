package com.chaekdojang.api.domain.dojangdan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewCampaignRepository extends JpaRepository<ReviewCampaign, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ReviewCampaign c where c.id = :id")
    Optional<ReviewCampaign> findForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from ReviewCampaign c where c.id = :id")
    Optional<ReviewCampaign> findForRead(@Param("id") Long id);

    @Query("""
            select c.id from ReviewCampaign c
            where c.status = com.chaekdojang.api.domain.dojangdan.CampaignStatus.RECRUITING
              and c.priorityInvitesSentAt is null and c.priorityInviteSender is not null
              and c.priorityInviteHours > 0 and c.recruitStartAt <= :now
              and c.recruitEndAt > :now and c.priorityInviteUntil > :now
            """)
    List<Long> findPendingInviteCampaignIds(@Param("now") java.time.LocalDateTime now);

    List<ReviewCampaign> findByStatusInOrderByRecruitEndAtDesc(Collection<CampaignStatus> statuses);

    List<ReviewCampaign> findByProfileIdInOrderByCreatedAtDesc(Collection<Long> profileIds);
}
