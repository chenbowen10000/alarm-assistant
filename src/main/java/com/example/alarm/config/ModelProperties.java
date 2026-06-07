package com.example.alarm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "alarm.models")
public class ModelProperties {

    private ModelConfig primary;
    private ModelConfig fallback;

    public ModelConfig getPrimary() { return primary; }
    public void setPrimary(ModelConfig primary) { this.primary = primary; }

    public ModelConfig getFallback() { return fallback; }
    public void setFallback(ModelConfig fallback) { this.fallback = fallback; }

    public static class ModelConfig {
        private String provider;
        private String apiKey;
        private String baseUrl;
        private String model;
        private double temperature = 0.3;
        private int maxTokens = 2000;
        private String timeout = "30s";

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }

        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

        public String getTimeout() { return timeout; }
        public void setTimeout(String timeout) { this.timeout = timeout; }

        public boolean isValid() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
