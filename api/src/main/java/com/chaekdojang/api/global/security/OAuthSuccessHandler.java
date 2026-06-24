package com.chaekdojang.api.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그인이 성공하면 Spring Security가 이 핸들러를 호출한다.
 * JWT를 만들어서 프론트엔드 콜백 페이지로 리다이렉트한다.
 */
@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthSessionService authSessionService;
    private final AuthCookieService authCookieService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuthUserPrincipal principal = (OAuthUserPrincipal) authentication.getPrincipal();
        authCookieService.addSession(response, authSessionService.createSession(principal.getUserId()));
        String redirect = principal.isNew()
                ? frontendUrl + "/auth/callback?setup=true"
                : frontendUrl + "/auth/callback";
        response.sendRedirect(redirect);
    }
}
