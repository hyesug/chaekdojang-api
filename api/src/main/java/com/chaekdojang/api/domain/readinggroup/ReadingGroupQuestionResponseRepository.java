package com.chaekdojang.api.domain.readinggroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReadingGroupQuestionResponseRepository extends JpaRepository<ReadingGroupQuestionResponse, Long> {
    List<ReadingGroupQuestionResponse> findAllByQuestionIdInOrderByCreatedAtAsc(Collection<Long> questionIds);
    Optional<ReadingGroupQuestionResponse> findByQuestionIdAndUserId(Long questionId, Long userId);
}
