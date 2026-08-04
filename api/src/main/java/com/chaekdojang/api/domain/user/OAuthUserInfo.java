package com.chaekdojang.api.domain.user;

import java.util.Map;

public record OAuthUserInfo(
        String providerId,
        String email,
        String nickname,
        String profileImage,
        AuthProvider provider
) {
    @SuppressWarnings("unchecked")
    public static OAuthUserInfo of(AuthProvider provider, Map<String, Object> attrs) {
        return switch (provider) {
            case KAKAO -> {
                String id = String.valueOf(attrs.get("id"));
                Map<String, Object> account = (Map<String, Object>) attrs.get("kakao_account");
                String email = account != null ? (String) account.get("email") : null;
                Map<String, Object> profile = account != null ? (Map<String, Object>) account.get("profile") : null;
                String nickname = profile != null ? (String) profile.get("nickname") : null;
                String image = profile != null ? (String) profile.get("profile_image_url") : null;
                yield new OAuthUserInfo(id, email, nickname, image, provider);
            }
            case NAVER -> {
                Map<String, Object> resp = (Map<String, Object>) attrs.get("response");
                yield new OAuthUserInfo(
                        (String) resp.get("id"),
                        null,
                        null,
                        null,
                        provider
                );
            }
            case GOOGLE -> new OAuthUserInfo(
                    (String) attrs.get("sub"),
                    (String) attrs.get("email"),
                    (String) attrs.get("name"),
                    (String) attrs.get("picture"),
                    provider
            );
            case LOCAL -> throw new IllegalStateException("LOCAL provider는 OAuth2 흐름으로 처리할 수 없습니다.");
        };
    }
}
