package com.chaekdojang.api.domain.review.ai;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewAiSummaryJobRepository extends JpaRepository<ReviewAiSummaryJob, Long> {

    List<ReviewAiSummaryJob> findByStatusOrderByIdAsc(ReviewAiSummaryStatus status, Pageable pageable);

    boolean existsByReviewIdAndStatusIn(Long reviewId, List<ReviewAiSummaryStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ReviewAiSummaryJob job
            SET job.status = :nextStatus,
                job.errorMessage = NULL,
                job.updatedAt = :updatedAt
            WHERE job.id = :jobId
              AND job.status = :expectedStatus
            """)
    int updateStatusIfCurrent(
            @Param("jobId") Long jobId,
            @Param("expectedStatus") ReviewAiSummaryStatus expectedStatus,
            @Param("nextStatus") ReviewAiSummaryStatus nextStatus,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ReviewAiSummaryJob job
            SET job.status = :status,
                job.retryCount = :retryCount,
                job.errorMessage = :errorMessage,
                job.updatedAt = :updatedAt,
                job.completedAt = :completedAt
            WHERE job.id = :jobId
            """)
    int finishJob(
            @Param("jobId") Long jobId,
            @Param("status") ReviewAiSummaryStatus status,
            @Param("retryCount") int retryCount,
            @Param("errorMessage") String errorMessage,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("completedAt") LocalDateTime completedAt);
}
