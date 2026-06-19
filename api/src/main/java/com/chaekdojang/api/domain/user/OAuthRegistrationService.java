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

    public record RegistrationResult(User user, boolean isNew) {}

    public RegistrationResult getOrRegister(AuthProvider provider, Map<String, Object> attributes) {
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
                return new RegistrationResult(linked, false);
            }
        }

        // 2. 같은 이메일로 다른 소셜 로그인을 한 계정이 있는지 확인 → 있으면 연결 (탈퇴 계정 제외)
        User user;
        boolean isNew = false;
        if (info.email() != null) {
            Optional<User> existingByEmail = userRepository.findByEmail(info.email());
            if (existingByEmail.isPresent() && existingByEmail.get().getDeletedAt() == null) {
                user = existingByEmail.get();
            } else {
                user = createNewUser(info);
                isNew = true;
            }
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

        return new RegistrationResult(user, isNew);
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
