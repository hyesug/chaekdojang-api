package com.chaekdojang.api.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByNicknameIgnoreCaseAndDeletedAtIsNull(String nickname);

    boolean existsByNickname(String nickname);

    List<User> findByNicknameContainingIgnoreCaseAndDeletedAtIsNull(String nickname);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    List<User> findAllByLifeBook_IdAndDeletedAtIsNull(Long lifeBookId);

    long countByCreatedAtBetweenAndDeletedAtIsNull(LocalDateTime start, LocalDateTime end);

    List<User> findAllByRoleInAndDeletedAtIsNull(List<UserRole> roles);
}
