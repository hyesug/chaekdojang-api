package com.chaekdojang.api.infra;

import com.chaekdojang.api.infra.google.GoogleBookResponse;
import com.chaekdojang.api.infra.kakao.KakaoBookResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookDescriptionResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void kakaoBookContentsIsReadAsDescriptionSource() throws Exception {
        KakaoBookResponse response = objectMapper.readValue("""
                {"documents":[{"title":"책","isbn":"1234567890123","authors":["작가"],
                "publisher":"출판사","thumbnail":"","contents":"간단한 책 소개",
                "sale_price":0,"category_name":"소설"}]}
                """, KakaoBookResponse.class);

        assertThat(response.documents().get(0).contents()).isEqualTo("간단한 책 소개");
    }

    @Test
    void googleBookDescriptionIsReadAsSynopsisSource() throws Exception {
        GoogleBookResponse response = objectMapper.readValue("""
                {"items":[{"volumeInfo":{"title":"책","authors":["작가"],"publisher":"출판사",
                "description":"<p>간단한 줄거리</p>","industryIdentifiers":[],"categories":[]}}]}
                """, GoogleBookResponse.class);

        assertThat(response.items().get(0).volumeInfo().description()).isEqualTo("<p>간단한 줄거리</p>");
    }
}
