package com.chaekdojang.api.domain.fortune;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app.ai-credit")
public class AiCreditProperties {
    private boolean grantExistingUsers = true;
    private BigDecimal inputCostPerMillionTokens = new BigDecimal("5");
    private BigDecimal outputCostPerMillionTokens = new BigDecimal("25");
    private BigDecimal cacheReadCostPerMillionTokens = new BigDecimal("0.50");
    private BigDecimal cacheWriteCostPerMillionTokens = new BigDecimal("6.25");
    public boolean isGrantExistingUsers() { return grantExistingUsers; }
    public void setGrantExistingUsers(boolean value) { grantExistingUsers = value; }
    public BigDecimal getInputCostPerMillionTokens() { return inputCostPerMillionTokens; }
    public void setInputCostPerMillionTokens(BigDecimal value) { inputCostPerMillionTokens = value; }
    public BigDecimal getOutputCostPerMillionTokens() { return outputCostPerMillionTokens; }
    public void setOutputCostPerMillionTokens(BigDecimal value) { outputCostPerMillionTokens = value; }
    public BigDecimal getCacheReadCostPerMillionTokens() { return cacheReadCostPerMillionTokens; }
    public void setCacheReadCostPerMillionTokens(BigDecimal value) { cacheReadCostPerMillionTokens = value; }
    public BigDecimal getCacheWriteCostPerMillionTokens() { return cacheWriteCostPerMillionTokens; }
    public void setCacheWriteCostPerMillionTokens(BigDecimal value) { cacheWriteCostPerMillionTokens = value; }
}
