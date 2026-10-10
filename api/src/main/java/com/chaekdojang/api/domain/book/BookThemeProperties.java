package com.chaekdojang.api.domain.book;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.book-theme")
public class BookThemeProperties {
    private boolean enabled = true;
    private String model = "gpt-5.5";
    private int batchSize = 20;
    private int descriptionChars = 400;
    private Duration timeout = Duration.ofSeconds(60);
    private int seedMinBooks = 8;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getDescriptionChars() { return descriptionChars; }
    public void setDescriptionChars(int descriptionChars) { this.descriptionChars = descriptionChars; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public int getSeedMinBooks() { return seedMinBooks; }
    public void setSeedMinBooks(int seedMinBooks) { this.seedMinBooks = seedMinBooks; }
}
