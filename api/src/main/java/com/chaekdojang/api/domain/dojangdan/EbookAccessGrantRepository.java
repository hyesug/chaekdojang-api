package com.chaekdojang.api.domain.dojangdan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EbookAccessGrantRepository extends JpaRepository<EbookAccessGrant, Long> {

    Optional<EbookAccessGrant> findByApplicationId(Long applicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from EbookAccessGrant g where g.application.id = :applicationId")
    Optional<EbookAccessGrant> findForUpdate(@Param("applicationId") Long applicationId);

    List<EbookAccessGrant> findByApplicationCampaignId(Long campaignId);

    List<EbookAccessGrant> findByApplicationIdIn(Collection<Long> applicationIds);
}
