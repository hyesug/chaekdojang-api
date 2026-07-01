package com.chaekdojang.api.domain.review.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewAiSummaryRepository extends JpaRepository<ReviewAiSummary, Long> {
    Optional<ReviewAiSummary> findByReviewId(Long reviewId);
    List<ReviewAiSummary> findAllByReviewIdIn(List<Long> reviewIds);
}
