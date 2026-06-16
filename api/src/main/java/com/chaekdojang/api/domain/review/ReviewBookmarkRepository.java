package com.chaekdojang.api.domain.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewBookmarkRepository extends JpaRepository<ReviewBookmark, Long> {

    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

    void deleteByReviewIdAndUserId(Long reviewId, Long userId);

    void deleteAllByUserId(Long userId);

    List<ReviewBookmark> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
