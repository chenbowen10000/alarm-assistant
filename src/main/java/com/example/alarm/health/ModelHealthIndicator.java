package com.example.alarm.health;

import com.example.alarm.config.ModelProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ModelHealthIndicator implements HealthIndicator {

    private final ModelProperties modelProperties;

    public ModelHealthIndicator(ModelProperties modelProperties) {
        this.modelProperties = modelProperties;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        ModelProperties.ModelConfig primary = modelProperties.getPrimary();
        ModelProperties.ModelConfig fallback = modelProperties.getFallback();

        builder.withDetail("primary.model", primary.getModel());
        builder.withDetail("primary.configured", primary.isValid());
        builder.withDetail("fallback.model", fallback.getModel());
        builder.withDetail("fallback.configured", fallback.isValid());

        if (!primary.isValid() && !fallback.isValid()) {
            builder.down().withDetail("warning", "No AI model API keys configured — rule engine only");
        }

        return builder.build();
    }
}
