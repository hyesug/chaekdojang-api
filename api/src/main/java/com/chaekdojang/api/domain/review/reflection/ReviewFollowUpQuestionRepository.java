package com.chaekdojang.api.domain.review.reflection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewFollowUpQuestionRepository extends JpaRepository<ReviewFollowUpQuestion, Long> {
    Optional<ReviewFollowUpQuestion> findByReviewId(Long reviewId);
    void deleteByReviewId(Long reviewId);
}
