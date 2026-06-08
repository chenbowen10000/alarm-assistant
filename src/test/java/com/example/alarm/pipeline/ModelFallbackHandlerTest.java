package com.example.alarm.pipeline;

import com.example.alarm.config.ModelProperties;
import com.example.alarm.config.ModelProperties.ModelConfig;
import com.example.alarm.pipeline.ModelFallbackHandler.ModelResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class ModelFallbackHandlerTest {

    private ModelFallbackHandler handler;

    @BeforeEach
    void setUp() {
        ModelProperties modelProperties = new ModelProperties();
        ModelConfig primary = new ModelConfig();
        primary.setProvider("deepseek");
        primary.setApiKey("");
        primary.setBaseUrl("https://api.deepseek.com");
        primary.setModel("deepseek-chat");
        modelProperties.setPrimary(primary);

        ModelConfig fallback = new ModelConfig();
        fallback.setProvider("qwen");
        fallback.setApiKey("");
        fallback.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode");
        fallback.setModel("qwen-turbo");
        modelProperties.setFallback(fallback);

        handler = new ModelFallbackHandler(modelProperties, new RestTemplate());
    }

    @Test
    void testRuleEngineFallbackWhenNoKeys() {
        ModelResult result = handler.callWithFallback("test system", "CPU 100% order-service timeout");
        assertNotNull(result);
        assertNotNull(result.getContent());
        assertTrue(result.getContent().length() > 50, "Rule engine should produce meaningful output");
        assertTrue(result.isFallbackActivated(), "Should be fallback activated");
        assertEquals("rule-engine", result.getModelUsed());
    }
}
