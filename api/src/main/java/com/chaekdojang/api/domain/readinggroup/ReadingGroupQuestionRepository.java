package com.chaekdojang.api.domain.readinggroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingGroupQuestionRepository extends JpaRepository<ReadingGroupQuestion, Long> {
    List<ReadingGroupQuestion> findAllByGroupBookIdOrderByCreatedAtAsc(Long groupBookId);
    Optional<ReadingGroupQuestion> findByIdAndGroupBookId(Long id, Long groupBookId);
}
