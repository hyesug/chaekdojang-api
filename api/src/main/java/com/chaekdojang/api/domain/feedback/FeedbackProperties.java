package com.chaekdojang.api.domain.feedback;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.feedback")
public class FeedbackProperties {
    private boolean enabled = true;
    private String apiKey = "";
    private String model = "";
    private String apiUrl = "https://api.openai.com/v1/responses";
    private int minChars = 150;
    private int maxChars = 6000;
    private int maxTokens = 2500;
    private Duration timeout = Duration.ofSeconds(30);
    private int memberDailyLimit = 5;
    private String boundaryMessage = "";
    private String betaApplyUrl = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public int getMinChars() { return minChars; }
    public void setMinChars(int minChars) { this.minChars = minChars; }
    public int getMaxChars() { return maxChars; }
    public void setMaxChars(int maxChars) { this.maxChars = maxChars; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public int getMemberDailyLimit() { return memberDailyLimit; }
    public void setMemberDailyLimit(int memberDailyLimit) { this.memberDailyLimit = memberDailyLimit; }
    public String getBoundaryMessage() { return boundaryMessage; }
    public void setBoundaryMessage(String boundaryMessage) { this.boundaryMessage = boundaryMessage; }
    public String getBetaApplyUrl() { return betaApplyUrl; }
    public void setBetaApplyUrl(String betaApplyUrl) { this.betaApplyUrl = betaApplyUrl; }
}
