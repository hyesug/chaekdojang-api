package com.chaekdojang.api.infra.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KakaoBookResponse(
        List<Document> documents
) {
    public record Document(
            String title,
            String isbn,
            List<String> authors,
            String publisher,
            String thumbnail,
            String contents,
            @JsonProperty("sale_price") int salePrice,
            @JsonProperty("category_name") String categoryName
    ) {}
}
