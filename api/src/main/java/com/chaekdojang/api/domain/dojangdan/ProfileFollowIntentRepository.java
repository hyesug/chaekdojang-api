package com.chaekdojang.api.domain.dojangdan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileFollowIntentRepository extends JpaRepository<ProfileFollowIntent, Long> {

    Optional<ProfileFollowIntent> findByUserIdAndProfileId(Long userId, Long profileId);

    List<ProfileFollowIntent> findByProfileIdAndUnsubscribedAtIsNull(Long profileId);

    List<ProfileFollowIntent> findByUserIdAndUnsubscribedAtIsNullOrderByCreatedAtDesc(Long userId);

    long countByProfileIdAndUnsubscribedAtIsNull(Long profileId);

    boolean existsByUserIdAndProfileIdAndUnsubscribedAtIsNull(Long userId, Long profileId);
}
