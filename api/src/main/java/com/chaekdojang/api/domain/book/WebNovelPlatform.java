package com.chaekdojang.api.domain.book;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum WebNovelPlatform {
    NAVER_SERIES(BookSource.NAVER_SERIES, "네이버 시리즈", "series.naver.com/novel"),
    KAKAO_PAGE(BookSource.KAKAO_PAGE, "카카오페이지", "page.kakao.com/content"),
    RIDI(BookSource.RIDI, "리디", "ridibooks.com/books"),
    MUNPIA(BookSource.MUNPIA, "문피아", "novel.munpia.com");

    private static final Pattern NUMBER_PATH = Pattern.compile("^/(\\d+)(?:/.*)?$");
    private static final Pattern CONTENT_PATH = Pattern.compile("^/content/(\\d+)(?:/.*)?$");
    private static final Pattern BOOK_PATH = Pattern.compile("^/books/(\\d+)(?:/.*)?$");
    private static final Pattern MUNPIA_MOBILE_PATH = Pattern.compile("^/novel/detail/(\\d+)(?:/.*)?$");
    private static final Pattern NAVER_WEB_NOVEL_PATH = Pattern.compile(
            "^/(webnovel|best|challenge)/(?:list|detail)(?:\\.(?:nhn|series))?$"
    );

    private final BookSource source;
    private final String label;
    private final String searchSite;

    WebNovelPlatform(BookSource source, String label, String searchSite) {
        this.source = source;
        this.label = label;
        this.searchSite = searchSite;
    }

    public BookSource source() {
        return source;
    }

    public String label() {
        return label;
    }

    public String labelFor(ResolvedWork work) {
        if (this == NAVER_SERIES && work.canonicalUrl().contains("novel.naver.com")) {
            return "네이버 웹소설";
        }
        return label;
    }

    public String searchQuery(String query) {
        return query + " site:" + searchSite;
    }

    public Optional<ResolvedWork> resolve(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl == null ? "" : rawUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http"))) {
                return Optional.empty();
            }
            host = host.toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath();
            return switch (this) {
                case NAVER_SERIES -> resolveNaver(host, path, uri.getRawQuery());
                case KAKAO_PAGE -> resolvePath(host.equals("page.kakao.com"), CONTENT_PATH, path,
                        id -> "https://page.kakao.com/content/" + id);
                case RIDI -> resolvePath(host.equals("ridibooks.com") || host.equals("www.ridibooks.com"), BOOK_PATH, path,
                        id -> "https://ridibooks.com/books/" + id);
                case MUNPIA -> resolveMunpia(host, path);
            };
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<WebNovelPlatform> fromSource(BookSource source) {
        return Arrays.stream(values()).filter(platform -> platform.source == source).findFirst();
    }

    private Optional<ResolvedWork> resolveNaver(String host, String path, String query) {
        if ((host.equals("series.naver.com") || host.equals("m.series.naver.com")) && path.startsWith("/novel/")) {
            String productNo = queryParameter(query, "productNo");
            if (productNo == null || !productNo.matches("\\d+")) return Optional.empty();
            return Optional.of(new ResolvedWork(productNo,
                    "https://series.naver.com/novel/detail.series?productNo=" + productNo));
        }

        if (!(host.equals("novel.naver.com") || host.equals("m.novel.naver.com"))) return Optional.empty();
        Matcher matcher = NAVER_WEB_NOVEL_PATH.matcher(path);
        if (!matcher.matches()) return Optional.empty();
        String novelId = queryParameter(query, "novelId");
        if (novelId == null || !novelId.matches("\\d+")) return Optional.empty();
        String section = matcher.group(1);
        return Optional.of(new ResolvedWork("webnovel-" + novelId,
                "https://novel.naver.com/" + section + "/list?novelId=" + novelId));
    }

    private Optional<ResolvedWork> resolveMunpia(String host, String path) {
        if (host.equals("novel.munpia.com")) {
            return resolvePath(true, NUMBER_PATH, path, id -> "https://novel.munpia.com/" + id);
        }
        if (host.equals("www.munpia.com") || host.equals("m.munpia.com") || host.equals("mm.munpia.com")) {
            return resolvePath(true, MUNPIA_MOBILE_PATH, path, id -> "https://novel.munpia.com/" + id);
        }
        return Optional.empty();
    }

    private Optional<ResolvedWork> resolvePath(
            boolean allowedHost,
            Pattern pattern,
            String path,
            java.util.function.Function<String, String> canonicalUrl
    ) {
        if (!allowedHost) return Optional.empty();
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) return Optional.empty();
        String id = matcher.group(1);
        return Optional.of(new ResolvedWork(id, canonicalUrl.apply(id)));
    }

    private String queryParameter(String query, String name) {
        if (query == null || query.isBlank()) return null;
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equals(name)) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    public record ResolvedWork(String externalId, String canonicalUrl) {
    }
}
