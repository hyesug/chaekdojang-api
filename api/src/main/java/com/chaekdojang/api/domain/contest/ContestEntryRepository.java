package com.chaekdojang.api.domain.contest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContestEntryRepository extends JpaRepository<ContestEntry, Long> {

    Optional<ContestEntry> findByContestIdAndUserId(Long contestId, Long userId);

    List<ContestEntry> findByContestIdOrderBySubmittedAtAsc(Long contestId);

    List<ContestEntry> findByContestIdAndStatusOrderByAwardRankAsc(Long contestId, ContestEntryStatus status);

    List<ContestEntry> findByUserIdOrderBySubmittedAtDesc(Long userId);

    long countByContestIdAndStatus(Long contestId, ContestEntryStatus status);

    long countByContestIdAndStatusNot(Long contestId, ContestEntryStatus status);
}
