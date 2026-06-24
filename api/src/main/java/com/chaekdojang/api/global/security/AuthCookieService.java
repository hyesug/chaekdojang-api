package com.chaekdojang.api.global.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthCookieService {

    public static final String ACCESS_TOKEN_COOKIE = "chaekdojang_access";
    public static final String REFRESH_TOKEN_COOKIE = "chaekdojang_refresh";

    private final long expirationMs;
    private final long refreshExpirationDays;
    private final boolean secureCookie;

    public AuthCookieService(
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${jwt.refresh-expiration-days:30}") long refreshExpirationDays,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl
    ) {
        this.expirationMs = expirationMs;
        this.refreshExpirationDays = refreshExpirationDays;
        this.secureCookie = frontendUrl != null && frontendUrl.startsWith("https://");
    }

    public void addAccessToken(HttpServletResponse response, String token) {
        ResponseCookie cookie = baseCookie(ACCESS_TOKEN_COOKIE, token)
                .maxAge(Duration.ofMillis(expirationMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAccessToken(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie(ACCESS_TOKEN_COOKIE, "")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addRefreshToken(HttpServletResponse response, String token) {
        ResponseCookie cookie = baseCookie(REFRESH_TOKEN_COOKIE, token)
                .maxAge(Duration.ofDays(refreshExpirationDays))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshToken(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie(REFRESH_TOKEN_COOKIE, "")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addSession(HttpServletResponse response, AuthSessionService.AuthTokens tokens) {
        addAccessToken(response, tokens.accessToken());
        addRefreshToken(response, tokens.refreshToken());
    }

    public void clearSession(HttpServletResponse response) {
        clearAccessToken(response);
        clearRefreshToken(response);
    }

    public Optional<String> readAccessToken(HttpServletRequest request) {
        return readCookie(request, ACCESS_TOKEN_COOKIE);
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return readCookie(request, REFRESH_TOKEN_COOKIE);
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/");
    }
}
