package com.chaekdojang.api.domain.readinggroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReadingGroupReviewRepository extends JpaRepository<ReadingGroupReview, Long> {
    List<ReadingGroupReview> findAllByGroupBookIdOrderByCreatedAtDesc(Long groupBookId);
    long countByGroupBookId(Long groupBookId);
    boolean existsByGroupBookIdAndReviewId(Long groupBookId, Long reviewId);
}
