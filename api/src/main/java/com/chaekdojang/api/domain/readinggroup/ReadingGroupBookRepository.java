package com.chaekdojang.api.domain.readinggroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingGroupBookRepository extends JpaRepository<ReadingGroupBook, Long> {
    List<ReadingGroupBook> findAllByGroupIdOrderByCreatedAtDesc(Long groupId);
    Optional<ReadingGroupBook> findByIdAndGroupId(Long id, Long groupId);
    boolean existsByGroupIdAndBookId(Long groupId, Long bookId);
    long countByGroupId(Long groupId);
}
