package com.chaekdojang.api.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("Authenticated user is required.");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Authenticated user id is required.", e);
            }
        }
        throw new IllegalStateException("Unsupported principal type: "
                + (principal == null ? "null" : principal.getClass().getName()));
    }
}
