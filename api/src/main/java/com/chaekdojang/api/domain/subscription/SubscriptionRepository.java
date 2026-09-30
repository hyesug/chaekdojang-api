package com.chaekdojang.api.domain.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserIdAndActiveTrue(Long userId);
    boolean existsByUserIdAndActiveTrue(Long userId);
    @Modifying
    @Query("UPDATE Subscription s SET s.active = false WHERE s.user.id = :userId AND s.active = true")
    int deactivateAllActiveByUserId(@Param("userId") Long userId);
}
