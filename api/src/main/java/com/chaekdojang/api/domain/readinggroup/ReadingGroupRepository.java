package com.chaekdojang.api.domain.readinggroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingGroupRepository extends JpaRepository<ReadingGroup, Long> {
    Optional<ReadingGroup> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<ReadingGroup> findAllByVisibilityOrderByCreatedAtDesc(ReadingGroupVisibility visibility);
    List<ReadingGroup> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
