package com.chaekdojang.api.domain.dojangdan;

public enum CampaignDeliveryType {
    PHYSICAL, // 실물 도서 배송
    PDF,      // PDF 파일 제공
    EPUB;     // EPUB 파일 제공

    public boolean isEbook() {
        return this != PHYSICAL;
    }
}
