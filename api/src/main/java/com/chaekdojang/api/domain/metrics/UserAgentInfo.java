package com.chaekdojang.api.domain.metrics;

import java.util.Locale;

public record UserAgentInfo(String device, String browser, String operatingSystem) {

    public static UserAgentInfo parse(String rawUserAgent) {
        String userAgent = rawUserAgent == null ? "" : rawUserAgent;
        String value = userAgent.toLowerCase(Locale.ROOT);

        String device = value.contains("ipad") || value.contains("tablet")
                || (value.contains("android") && !value.contains("mobile"))
                ? "tablet"
                : value.contains("mobile") || value.contains("iphone") || value.contains("android")
                ? "mobile"
                : "desktop";

        String browser;
        if (value.contains("edg/")) browser = "Edge";
        else if (value.contains("samsungbrowser/")) browser = "Samsung Internet";
        else if (value.contains("opr/") || value.contains("opera")) browser = "Opera";
        else if (value.contains("chrome/") || value.contains("crios/")) browser = "Chrome";
        else if (value.contains("firefox/") || value.contains("fxios/")) browser = "Firefox";
        else if (value.contains("safari/") && !value.contains("chrome/")) browser = "Safari";
        else browser = "기타";

        String operatingSystem;
        if (value.contains("windows")) operatingSystem = "Windows";
        else if (value.contains("android")) operatingSystem = "Android";
        else if (value.contains("iphone") || value.contains("ipad") || value.contains("cpu os")) operatingSystem = "iOS";
        else if (value.contains("mac os") || value.contains("macintosh")) operatingSystem = "macOS";
        else if (value.contains("linux")) operatingSystem = "Linux";
        else operatingSystem = "기타";

        return new UserAgentInfo(device, browser, operatingSystem);
    }
}
