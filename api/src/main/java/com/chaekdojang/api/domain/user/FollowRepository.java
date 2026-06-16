package com.chaekdojang.api.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteAllByFollowerIdOrFollowingId(Long followerId, Long followingId);

    List<Follow> findAllByFollowingIdOrderByCreatedAtDesc(Long followingId);

    List<Follow> findAllByFollowerIdOrderByCreatedAtDesc(Long followerId);

    long countByFollowingId(Long followingId);

    long countByFollowerId(Long followerId);

    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :followerId")
    List<Long> findFollowingIdsByFollowerId(@Param("followerId") Long followerId);
}
