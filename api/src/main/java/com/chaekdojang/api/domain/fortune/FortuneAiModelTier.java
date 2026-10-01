package com.chaekdojang.api.domain.fortune;

public enum FortuneAiModelTier {
    CLAUDE_SONNET("Claude 균형 풀이", "ANTHROPIC", "claude-sonnet-5-5", 1),
    GPT_SOL("GPT 균형 풀이", "OPENAI", "gpt-6.1-sol", 1),
    CLAUDE_OPUS("Claude 심층 풀이", "ANTHROPIC", "claude-opus-5-5", 2),
    GPT_ASTRA("GPT 최고 심층 풀이", "OPENAI", "gpt-6-astra", 5);

    private final String displayName;
    private final String provider;
    private final String model;
    private final int creditCost;
    FortuneAiModelTier(String displayName, String provider, String model, int creditCost) { this.displayName = displayName; this.provider = provider; this.model = model; this.creditCost = creditCost; }
    public String displayName() { return displayName; }
    public String provider() { return provider; }
    public String model() { return model; }
    public int creditCost() { return creditCost; }
}
