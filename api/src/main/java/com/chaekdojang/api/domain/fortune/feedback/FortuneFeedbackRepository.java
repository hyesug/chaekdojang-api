package com.chaekdojang.api.domain.fortune.feedback;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FortuneFeedbackRepository extends JpaRepository<FortuneFeedback, Long> {

    List<FortuneFeedback> findAllByOrderByIdDesc(Pageable pageable);

    List<FortuneFeedback> findByResolvedAtIsNullOrderByIdDesc(Pageable pageable);

    /** 칸별 👍/👎 수 — [section, verdict, count] (처리한 것은 빼고) */
    @Query("select f.section, f.verdict, count(f) from FortuneFeedback f where f.resolvedAt is null group by f.section, f.verdict order by f.section")
    List<Object[]> countBySectionAndVerdict();
}
