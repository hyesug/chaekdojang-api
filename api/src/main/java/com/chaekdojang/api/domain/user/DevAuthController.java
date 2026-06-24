package com.chaekdojang.api.domain.user;

import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.AuthCookieService;
import com.chaekdojang.api.global.security.AuthSessionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevAuthController {

    private static final String DEV_EMAIL = "local-dev@chaekdojang.test";
    private static final String DEV_NICKNAME = "로컬독자";

    private final UserRepository userRepository;
    private final AuthSessionService authSessionService;
    private final AuthCookieService authCookieService;

    @Value("${app.dev-login-enabled:false}")
    private boolean devLoginEnabled;

    @PostMapping("/login")
    public ApiResponse<DevLoginResponse> login(HttpServletResponse response) {
        if (!devLoginEnabled) {
            throw new ResponseStatusException(NOT_FOUND);
        }

        User user = userRepository.findByEmail(DEV_EMAIL)
                .orElseGet(() -> userRepository.save(User.create(DEV_EMAIL, uniqueNickname(), null)));
        if (!user.isSuperAdmin()) {
            user.setSuperAdmin();
            user = userRepository.save(user);
        }
        AuthSessionService.AuthTokens tokens = authSessionService.createSession(user.getId());
        authCookieService.addSession(response, tokens);
        return ApiResponse.ok(new DevLoginResponse(user.getId(), user.getNickname()));
    }

    private String uniqueNickname() {
        if (!userRepository.existsByNickname(DEV_NICKNAME)) return DEV_NICKNAME;
        return DEV_NICKNAME + "_" + System.currentTimeMillis();
    }

    public record DevLoginResponse(Long userId, String nickname) {
    }
}
