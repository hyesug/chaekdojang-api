package com.chaekdojang.api.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SuspiciousPathBlockFilter extends OncePerRequestFilter {

    private static final List<String> BLOCKED_EXACT_PATHS = List.of(
            "/.env",
            "/debug.log",
            "/shell.php",
            "/backdoor.php",
            "/wp-login.php",
            "/xmlrpc.php"
    );

    private static final List<String> BLOCKED_PREFIXES = List.of(
            "/.git",
            "/.svn",
            "/phpmyadmin",
            "/vendor",
            "/server-status"
    );

    private static final List<String> BLOCKED_CONTAINS = List.of(
            "../",
            "..%2f",
            "%2e%2e",
            "\\",
            "%5c"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = normalizePath(request.getRequestURI());

        if (isSuspicious(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSuspicious(String path) {
        String lowerPath = path.toLowerCase(Locale.ROOT);

        if (BLOCKED_EXACT_PATHS.contains(lowerPath)) {
            return true;
        }

        if (BLOCKED_PREFIXES.stream().anyMatch(lowerPath::startsWith)) {
            return true;
        }

        if (BLOCKED_CONTAINS.stream().anyMatch(lowerPath::contains)) {
            return true;
        }

        return lowerPath.endsWith(".php")
                || lowerPath.endsWith(".bak")
                || lowerPath.endsWith(".sql")
                || lowerPath.endsWith(".tar")
                || lowerPath.endsWith(".gz")
                || lowerPath.endsWith(".zip");
    }

    private String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }
        String path = rawPath.trim();
        return path.startsWith("/") ? path : "/" + path;
    }
}
