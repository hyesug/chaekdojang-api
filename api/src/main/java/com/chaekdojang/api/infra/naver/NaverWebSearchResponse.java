package com.chaekdojang.api.infra.naver;

import java.util.List;

public record NaverWebSearchResponse(List<Item> items) {
    public record Item(String title, String link, String description) {
    }
}
