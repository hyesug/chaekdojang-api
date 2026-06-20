package com.chaekdojang.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int apiLimitPerMinute = 600;
    private int uploadLimitPerMinute = 20;
    private int authLimitPerMinute = 30;
    private int metricsLimitPerMinute = 240;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getApiLimitPerMinute() {
        return apiLimitPerMinute;
    }

    public void setApiLimitPerMinute(int apiLimitPerMinute) {
        this.apiLimitPerMinute = apiLimitPerMinute;
    }

    public int getUploadLimitPerMinute() {
        return uploadLimitPerMinute;
    }

    public void setUploadLimitPerMinute(int uploadLimitPerMinute) {
        this.uploadLimitPerMinute = uploadLimitPerMinute;
    }

    public int getAuthLimitPerMinute() {
        return authLimitPerMinute;
    }

    public void setAuthLimitPerMinute(int authLimitPerMinute) {
        this.authLimitPerMinute = authLimitPerMinute;
    }

    public int getMetricsLimitPerMinute() {
        return metricsLimitPerMinute;
    }

    public void setMetricsLimitPerMinute(int metricsLimitPerMinute) {
        this.metricsLimitPerMinute = metricsLimitPerMinute;
    }
}
