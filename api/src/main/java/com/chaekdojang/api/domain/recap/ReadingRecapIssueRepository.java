package com.chaekdojang.api.domain.recap;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingRecapIssueRepository extends JpaRepository<ReadingRecapIssue, Long> {
    boolean existsByUserIdAndPeriodKey(Long userId, String periodKey);
    Optional<ReadingRecapIssue> findByUserIdAndPeriodKey(Long userId, String periodKey);
    List<ReadingRecapIssue> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ReadingRecapIssue> findByIdAndUserId(Long id, Long userId);
}
