package com.chaekdojang.api.domain.user;

import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.AuthCookieService;
import com.chaekdojang.api.global.security.AuthSessionService;
import com.chaekdojang.api.global.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCookieService authCookieService;
    private final AuthSessionService authSessionService;

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authCookieService.readRefreshToken(request).ifPresent(authSessionService::revoke);
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        if (userId != null) {
            authSessionService.revokeAll(userId);
        }
        authCookieService.clearSession(response);
        return ApiResponse.ok(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<SessionResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authCookieService.readRefreshToken(request)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));
        authCookieService.addSession(response, authSessionService.rotate(refreshToken));
        return ApiResponse.ok(new SessionResponse(true));
    }

    @GetMapping("/session")
    public ApiResponse<SessionResponse> session() {
        return ApiResponse.ok(new SessionResponse(SecurityUtils.getCurrentUserIdOrNull() != null));
    }

    public record SessionResponse(boolean authenticated) {
    }
}
