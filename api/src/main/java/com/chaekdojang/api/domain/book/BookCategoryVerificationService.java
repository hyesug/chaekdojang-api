package com.chaekdojang.api.domain.book;

import com.chaekdojang.api.infra.wikidata.WikidataBookCategoryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class BookCategoryVerificationService {
    private final WikidataBookCategoryClient wikidataBookCategoryClient;
    private final StringRedisTemplate redisTemplate;

    public void verifyIfNeeded(Book book) {
        if (book == null || book.isWebNovel() || book.isCategoryVerified()) return;
        String cacheKey = "book:category-authority-miss:v1:" + book.getId();
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) return;
        } catch (RuntimeException ignored) {
            // 외부 분류 확인은 Redis 장애와 무관하게 계속할 수 있다.
        }

        String verified = wikidataBookCategoryClient.findVerifiedCategory(book.getTitle(), book.getAuthor());
        if (verified != null) {
            book.updateVerifiedCategory(verified);
            return;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, "1", Duration.ofDays(7));
        } catch (RuntimeException ignored) {
            // 조회 실패 캐시는 선택 사항이다.
        }
    }
}
