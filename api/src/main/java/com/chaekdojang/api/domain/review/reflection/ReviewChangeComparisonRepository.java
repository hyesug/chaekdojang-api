package com.chaekdojang.api.domain.review.reflection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewChangeComparisonRepository extends JpaRepository<ReviewChangeComparison, Long> {
    Optional<ReviewChangeComparison> findByReviewId(Long reviewId);
    void deleteByReviewId(Long reviewId);
}
