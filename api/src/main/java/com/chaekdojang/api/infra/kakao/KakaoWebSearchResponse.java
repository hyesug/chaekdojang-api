package com.chaekdojang.api.infra.kakao;

import java.util.List;

public record KakaoWebSearchResponse(List<Document> documents) {
    public record Document(String title, String contents, String url) {
    }
}
