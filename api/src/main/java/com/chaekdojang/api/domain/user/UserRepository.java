package com.chaekdojang.api.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByNicknameIgnoreCaseAndDeletedAtIsNull(String nickname);

    boolean existsByNickname(String nickname);

    List<User> findByNicknameContainingIgnoreCase(String nickname);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    List<User> findAllByLifeBook_IdAndDeletedAtIsNull(Long lifeBookId);
}
