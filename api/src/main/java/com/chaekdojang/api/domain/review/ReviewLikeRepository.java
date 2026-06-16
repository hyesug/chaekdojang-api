package com.chaekdojang.api.domain.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

    void deleteByReviewIdAndUserId(Long reviewId, Long userId);

    void deleteAllByUserId(Long userId);

    long countByReviewId(Long reviewId);

    // 리뷰 ID 목록을 한 번의 쿼리로 카운트 — N+1 방지
    @Query("SELECT rl.review.id, COUNT(rl) FROM ReviewLike rl WHERE rl.review.id IN :ids GROUP BY rl.review.id")
    List<Object[]> countGroupByReviewIds(@Param("ids") List<Long> ids);
}
