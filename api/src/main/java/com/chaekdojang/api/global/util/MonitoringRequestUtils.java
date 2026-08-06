package com.chaekdojang.api.global.util;

import jakarta.servlet.http.HttpServletRequest;

public final class MonitoringRequestUtils {

    public static final String HEADER = "X-Chaekdojang-Internal-Request";
    public static final String CODEX_MONITOR = "codex-monitor";
    public static final String WEB_SSR = "web-ssr";

    private MonitoringRequestUtils() {
    }

    public static boolean isCodexMonitor(HttpServletRequest request) {
        return request != null && CODEX_MONITOR.equals(request.getHeader(HEADER));
    }

    public static boolean isInternalAccess(HttpServletRequest request) {
        if (request == null) return false;
        String marker = request.getHeader(HEADER);
        return WEB_SSR.equals(marker) || CODEX_MONITOR.equals(marker);
    }
}
