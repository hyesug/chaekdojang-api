package com.chaekdojang.api.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {

    Optional<UserAuthProvider> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    void deleteAllByUserId(Long userId);

    List<UserAuthProvider> findAllByUserIdOrderByCreatedAtAsc(Long userId);
}
