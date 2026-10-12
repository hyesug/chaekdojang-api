package com.chaekdojang.api.domain.fortune;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FortuneProfileRepository extends JpaRepository<FortuneProfile, Long> {
    /** 내 프로필(본인) */
    Optional<FortuneProfile> findByUserIdAndSelfTrue(Long userId);

    /** 본인을 맨 앞에, 나머지는 만든 순서대로 */
    List<FortuneProfile> findAllByUserIdOrderBySelfDescCreatedAtAscIdAsc(Long userId);

    Optional<FortuneProfile> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    /** 회원 탈퇴 — 그 사람이 저장한 프로필 전부 */
    void deleteByUserId(Long userId);
}
