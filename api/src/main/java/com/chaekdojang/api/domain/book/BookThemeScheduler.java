package com.chaekdojang.api.domain.book;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 책 주제 태그 — 한 번 돌 때 후보 모으기(검색 한 번)와 태그 붙이기(AI 한 번)를 한다 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookThemeScheduler {

    private final BookThemeService service;
    private final BookThemeCurator curator;

    /** 스테디셀러 목록 등록 — 20초마다 4권씩 찾아 끝나면 멈춘다(120권이면 10분 남짓) */
    @Scheduled(fixedDelay = 20000, initialDelay = 30000)
    public void curate() {
        if (curator.finished()) return;
        try {
            int n = curator.tick(4);
            if (n > 0) log.info("book-theme: curated {} books", n);
        } catch (Exception e) {
            log.warn("book-theme curate failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${app.book-theme.scheduler-delay-ms:600000}", initialDelay = 60000)
    public void run() {
        try {
            service.seedThin();
            int n = service.tagPending();
            if (n > 0) log.info("book-theme: tagged {} books", n);
        } catch (Exception e) {
            log.warn("book-theme run failed: {}", e.getMessage());
        }
    }
}
