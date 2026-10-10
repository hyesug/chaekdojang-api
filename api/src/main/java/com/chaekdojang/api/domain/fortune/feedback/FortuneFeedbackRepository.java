package com.chaekdojang.api.domain.fortune.feedback;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FortuneFeedbackRepository extends JpaRepository<FortuneFeedback, Long> {

    List<FortuneFeedback> findAllByOrderByIdDesc(Pageable pageable);

    /** 칸별 👍/👎 수 — [section, verdict, count] */
    @Query("select f.section, f.verdict, count(f) from FortuneFeedback f group by f.section, f.verdict order by f.section")
    List<Object[]> countBySectionAndVerdict();
}
