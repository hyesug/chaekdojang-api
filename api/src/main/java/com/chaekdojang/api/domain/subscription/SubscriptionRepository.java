package com.chaekdojang.api.domain.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserIdAndActiveTrue(Long userId);
    boolean existsByUserIdAndActiveTrue(Long userId);
    void deleteAllByUserId(Long userId);
}
