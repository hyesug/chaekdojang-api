package com.chaekdojang.api.domain.readinggroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReadingGroupBookAiAnalysisRepository extends JpaRepository<ReadingGroupBookAiAnalysis, Long> {
    Optional<ReadingGroupBookAiAnalysis> findByGroupBookId(Long groupBookId);
}
