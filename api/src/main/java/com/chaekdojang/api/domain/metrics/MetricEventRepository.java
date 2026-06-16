package com.chaekdojang.api.domain.metrics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MetricEventRepository extends JpaRepository<MetricEvent, Long> {
    Page<MetricEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<MetricEvent> findTop1000ByUserIsNotNullAndIpIsNotNullOrderByCreatedAtDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MetricEvent m SET m.user = null WHERE m.user.id = :userId")
    void anonymizeUser(@Param("userId") Long userId);
}
