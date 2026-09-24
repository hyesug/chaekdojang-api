package com.chaekdojang.api.domain.fortune;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FortuneProfileRepository extends JpaRepository<FortuneProfile, Long> {
    Optional<FortuneProfile> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
