package com.chaekdojang.api.global.security;

import com.chaekdojang.api.domain.user.RefreshToken;
import com.chaekdojang.api.domain.user.RefreshTokenRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration-days:30}")
    private long refreshExpirationDays;

    @Transactional
    public AuthTokens createSession(Long userId) {
        User user = userRepository.findById(userId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String refreshToken = randomToken();
        refreshTokenRepository.save(RefreshToken.create(
                user,
                hash(refreshToken),
                LocalDateTime.now().plusDays(refreshExpirationDays)
        ));
        return new AuthTokens(jwtProvider.generate(userId), refreshToken);
    }

    @Transactional
    public AuthTokens rotate(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));
        if (!stored.isActive(LocalDateTime.now()) || stored.getUser().getDeletedAt() != null) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        Long userId = stored.getUser().getId();
        stored.revokeForRotation();
        return createSession(userId);
    }

    @Transactional
    public void revoke(String refreshToken) {
        refreshTokenRepository.findByTokenHash(hash(refreshToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId).forEach(RefreshToken::revoke);
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash refresh token.", e);
        }
    }

    public record AuthTokens(String accessToken, String refreshToken) {
    }
}
