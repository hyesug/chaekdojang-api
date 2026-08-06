package com.chaekdojang.api.domain.recap;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingRecapPreferenceRepository extends JpaRepository<ReadingRecapPreference, Long> {
    Optional<ReadingRecapPreference> findByUserId(Long userId);
    List<ReadingRecapPreference> findAllByEnabledTrue();
}
