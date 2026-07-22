package com.chaekdojang.api.infra.kakao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoWebNovelClientTest {

    private final KakaoWebNovelClient client = new KakaoWebNovelClient("test-key");

    @Test
    void matchesTitleFragmentInTheMiddle() {
        assertThat(client.titleMatches(
                "용꿈",
                "은행원도 용꿈을 꾸나요 - 판타지 웹소설"
        )).isTrue();
    }
}
