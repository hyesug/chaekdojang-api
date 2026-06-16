package com.chaekdojang.api.domain.metrics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricEventRepository extends JpaRepository<MetricEvent, Long> {
    Page<MetricEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<MetricEvent> findTop1000ByUserIsNotNullAndIpIsNotNullOrderByCreatedAtDesc();
}
