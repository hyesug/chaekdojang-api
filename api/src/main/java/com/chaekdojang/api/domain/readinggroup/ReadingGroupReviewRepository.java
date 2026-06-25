package com.chaekdojang.api.domain.readinggroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingGroupReviewRepository extends JpaRepository<ReadingGroupReview, Long> {
    List<ReadingGroupReview> findAllByGroupBookIdOrderByCreatedAtDesc(Long groupBookId);
    List<ReadingGroupReview> findAllByGroupIdOrderByCreatedAtDesc(Long groupId);
    long countByGroupBookId(Long groupBookId);
    Optional<ReadingGroupReview> findByGroupBookIdAndReviewId(Long groupBookId, Long reviewId);
    boolean existsByGroupBookIdAndReviewId(Long groupBookId, Long reviewId);
    void deleteAllByGroupId(Long groupId);
}
