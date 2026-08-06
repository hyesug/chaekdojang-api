package com.chaekdojang.api.infra.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KakaoBookResponse(
        List<Document> documents
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            String title,
            String isbn,
            List<String> authors,
            String publisher,
            String thumbnail,
            String contents,
            @JsonProperty("sale_price") int salePrice
    ) {}
}
