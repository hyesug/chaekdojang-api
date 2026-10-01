package com.chaekdojang.api.domain.fortune;

/** 판매 상품의 가격과 지급 질문권을 한 곳에서 관리한다. validDays가 0이면 만료되지 않는다. */
public enum AiCreditProduct {
    DAY_PASS("하루권", 5, 3_900, 1),
    WEEK_PASS("일주일권", 20, 12_900, 7),
    MONTH_PASS("한달권", 60, 29_900, 30),
    PACK_10("질문권 10회", 10, 5_900, 0),
    PACK_30("질문권 30회", 30, 15_900, 0),
    PACK_60("질문권 60회", 60, 29_900, 0);

    private final String displayName;
    private final int credits;
    private final int price;
    private final int validDays;

    AiCreditProduct(String displayName, int credits, int price, int validDays) {
        this.displayName = displayName;
        this.credits = credits;
        this.price = price;
        this.validDays = validDays;
    }

    public String displayName() { return displayName; }
    public int credits() { return credits; }
    public int price() { return price; }
    public int validDays() { return validDays; }
}
