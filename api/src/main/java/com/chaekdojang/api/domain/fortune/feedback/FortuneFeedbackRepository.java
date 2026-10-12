package com.chaekdojang.api.domain.fortune.feedback;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FortuneFeedbackRepository extends JpaRepository<FortuneFeedback, Long> {

    List<FortuneFeedback> findAllByOrderByIdDesc(Pageable pageable);

    List<FortuneFeedback> findByResolvedAtIsNullOrderByIdDesc(Pageable pageable);

    /** 칸별 👍/👎 수 — [section, verdict, count] (처리한 것은 빼고) */
    @Query("select f.section, f.verdict, count(f) from FortuneFeedback f where f.resolvedAt is null group by f.section, f.verdict order by f.section")
    List<Object[]> countBySectionAndVerdict();

    /** 보관 기간(1년)이 지난 피드백 삭제 — 개인정보처리방침에 적은 기간 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FortuneFeedback f WHERE f.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
