package com.chaekdojang.api.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthUserInfoTest {

    @Test
    @DisplayName("네이버 로그인은 고유 ID만 사용하고 개인정보는 저장하지 않는다")
    void naverUsesOnlyProviderId() {
        OAuthUserInfo info = OAuthUserInfo.of(AuthProvider.NAVER, Map.of(
                "response", Map.of(
                        "id", "naver-user-id",
                        "email", "user@example.com",
                        "name", "네이버 사용자",
                        "profile_image", "https://example.com/profile.jpg"
                )
        ));

        assertThat(info.providerId()).isEqualTo("naver-user-id");
        assertThat(info.email()).isNull();
        assertThat(info.nickname()).isNull();
        assertThat(info.profileImage()).isNull();
        assertThat(info.provider()).isEqualTo(AuthProvider.NAVER);
    }
}
