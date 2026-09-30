package com.chaekdojang.api.domain.user;

import com.chaekdojang.api.global.notification.AdminAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OAuthRegistrationService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository authProviderRepository;
    private final AdminAlertService adminAlertService;

    @Value("${admin.super-email:}")
    private String superAdminEmail;

    public record RegistrationResult(User user, boolean isNew, boolean linked) {}

    public RegistrationResult getOrRegister(AuthProvider provider, Map<String, Object> attributes) {
        return getOrRegister(provider, attributes, null);
    }

    public RegistrationResult getOrRegister(AuthProvider provider, Map<String, Object> attributes, Long linkUserId) {
        OAuthUserInfo info = OAuthUserInfo.of(provider, attributes);

        // 1. 이미 이 소셜 로그인 수단으로 가입된 계정이 있는지 확인
        Optional<UserAuthProvider> existingAuth =
                authProviderRepository.findByProviderAndProviderUserId(provider, info.providerId());
        if (existingAuth.isPresent()) {
            User linked = existingAuth.get().getUser();
            // 탈퇴 계정은 되살리지 않고 새 가입으로 처리한다.
            if (linked.getDeletedAt() != null) {
                authProviderRepository.delete(existingAuth.get());
                authProviderRepository.flush();
            } else {
                // 재로그인 시에도 SUPER_ADMIN 이메일이면 권한 부여 (최초 1회)
                if (superAdminEmail != null && !superAdminEmail.isBlank()
                        && superAdminEmail.equals(linked.getEmail()) && !linked.isSuperAdmin()) {
                    linked.setSuperAdmin();
                }
                if (linkUserId != null && !linked.getId().equals(linkUserId)) {
                    throw new com.chaekdojang.api.global.exception.CustomException(
                            com.chaekdojang.api.global.exception.ErrorCode.AUTH_PROVIDER_ALREADY_LINKED);
                }
                return new RegistrationResult(linked, false, false);
            }
        }

        // 이메일은 계정 소유 증명이 아니므로 다른 소셜 로그인과 자동 병합하지 않는다.
        User user;
        boolean isNew = false;
        boolean linked = linkUserId != null;
        if (linked) {
            user = userRepository.findById(linkUserId)
                    .filter(item -> item.getDeletedAt() == null)
                    .orElseThrow(() -> new com.chaekdojang.api.global.exception.CustomException(
                            com.chaekdojang.api.global.exception.ErrorCode.USER_NOT_FOUND));
        } else {
            user = createNewUser(info);
            isNew = true;
        }

        // 3. 소셜 로그인 수단 연결
        authProviderRepository.save(
                UserAuthProvider.of(user, provider, info.providerId(), info.email(), info.profileImage())
        );

        // SUPER_ADMIN 이메일이면 자동으로 슈퍼 관리자 권한 부여
        if (superAdminEmail != null && !superAdminEmail.isBlank()
                && superAdminEmail.equals(user.getEmail()) && !user.isSuperAdmin()) {
            user.setSuperAdmin();
        }

        if (isNew) {
            adminAlertService.sendSignupAlert(user.getEmail(), user.getNickname());
        }

        return new RegistrationResult(user, isNew, linked);
    }

    private User createNewUser(OAuthUserInfo info) {
        String nickname = makeUniqueNickname("독자");
        return userRepository.save(
                User.create(info.email(), nickname, info.profileImage())
        );
    }

    private String makeUniqueNickname(String base) {
        String candidate = (base != null && !base.isBlank()) ? base : "독자";
        if (!userRepository.existsByNickname(candidate)) return candidate;
        for (int i = 0; i < 5; i++) {
            String s = candidate + "_" + UUID.randomUUID().toString().substring(0, 4);
            if (!userRepository.existsByNickname(s)) return s;
        }
        return candidate + "_" + System.currentTimeMillis();
    }
}
