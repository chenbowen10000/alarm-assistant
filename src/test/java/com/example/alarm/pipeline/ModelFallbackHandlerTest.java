package com.example.alarm.pipeline;

import com.example.alarm.config.ModelProperties;
import com.example.alarm.config.ModelProperties.ModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelFallbackHandlerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RestTemplate restTemplate;

    private ModelProperties modelProperties;
    private ModelFallbackHandler handler;

    private static final String SYSTEM_PROMPT = "You are an alarm analyst";
    private static final String USER_PROMPT = "order-service timeout alarm";
    private static final String MOCK_RESPONSE_CONTENT = "Analysis: order-service is experiencing timeout due to high CPU";

    @BeforeEach
    void setUp() {
        modelProperties = new ModelProperties();
    }

    private void setupPrimaryConfig(String apiKey) {
        ModelConfig primary = new ModelConfig();
        primary.setProvider("deepseek");
        primary.setApiKey(apiKey);
        primary.setBaseUrl("https://api.deepseek.com");
        primary.setModel("deepseek-chat");
        primary.setTemperature(0.3);
        primary.setMaxTokens(2000);
        modelProperties.setPrimary(primary);
    }

    private void setupFallbackConfig(String apiKey) {
        ModelConfig fallback = new ModelConfig();
        fallback.setProvider("qwen");
        fallback.setApiKey(apiKey);
        fallback.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode");
        fallback.setModel("qwen-turbo");
        fallback.setTemperature(0.3);
        fallback.setMaxTokens(2000);
        modelProperties.setFallback(fallback);
    }

    private void mockModelSuccess(String content) {
        ObjectNode responseBody = objectMapper.createObjectNode();
        ObjectNode choice = responseBody.putArray("choices").addObject();
        choice.put("index", 0);
        choice.putObject("message").put("role", "assistant").put("content", content);

        ResponseEntity<JsonNode> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(response);
    }

    private void mockModelFailure() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new RestClientException("Connection refused"));
    }

    // ======== Test: Primary succeeds ========
    @Test
    void callWithFallback_primarySucceeds_returnsPrimaryResult() {
        setupPrimaryConfig("sk-test-primary-key");
        setupFallbackConfig("sk-test-fallback-key");
        handler = new ModelFallbackHandler(modelProperties, restTemplate);
        mockModelSuccess(MOCK_RESPONSE_CONTENT);

        ModelFallbackHandler.ModelResult result = handler.callWithFallback(SYSTEM_PROMPT, USER_PROMPT);

        assertEquals(MOCK_RESPONSE_CONTENT, result.getContent());
        assertEquals("deepseek-chat", result.getModelUsed());
        assertFalse(result.isFallbackActivated());

        // Verify RestTemplate was called exactly once
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class));
        verify(restTemplate, times(1)).setRequestFactory(any());
    }

    // ======== Test: Primary fails, fallback succeeds ========
    @Test
    void callWithFallback_primaryFailsFallbackSucceeds_returnsFallbackResult() {
        setupPrimaryConfig("sk-test-primary-key");
        setupFallbackConfig("sk-test-fallback-key");
        handler = new ModelFallbackHandler(modelProperties, restTemplate);

        // Primary throws, fallback returns success
        String fallbackContent = "Fallback analysis: order-service degraded";
        ObjectNode responseBody = objectMapper.createObjectNode();
        ObjectNode choice = responseBody.putArray("choices").addObject();
        choice.put("index", 0);
        choice.putObject("message").put("role", "assistant").put("content", fallbackContent);
        ResponseEntity<JsonNode> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new RestClientException("Primary timeout"))
                .thenReturn(response);

        ModelFallbackHandler.ModelResult result = handler.callWithFallback(SYSTEM_PROMPT, USER_PROMPT);

        assertEquals(fallbackContent, result.getContent());
        assertEquals("qwen-turbo", result.getModelUsed());
        assertTrue(result.isFallbackActivated());

        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class));
    }

    // ======== Test: Both fail, rule engine used ========
    @Test
    void callWithFallback_bothModelsFail_usesRuleEngine() {
        setupPrimaryConfig("sk-test-primary-key");
        setupFallbackConfig("sk-test-fallback-key");
        handler = new ModelFallbackHandler(modelProperties, restTemplate);
        mockModelFailure();

        ModelFallbackHandler.ModelResult result = handler.callWithFallback(SYSTEM_PROMPT, USER_PROMPT);

        assertEquals("rule-engine", result.getModelUsed());
        assertTrue(result.isFallbackActivated());
        assertNotNull(result.getContent());
        // Rule engine should have detected order-service and timeout
        assertTrue(result.getContent().contains("order-service"));
        assertTrue(result.getContent().contains("规则引擎"));

        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class));
    }

    // ======== Test: Primary API key missing, skips to fallback ========
    @Test
    void callWithFallback_primaryApiKeyMissing_skipsToFallback() {
        setupPrimaryConfig(null); // No API key
        setupFallbackConfig("sk-test-fallback-key");
        handler = new ModelFallbackHandler(modelProperties, restTemplate);

        String fallbackContent = "Qwen analysis result";
        ObjectNode responseBody = objectMapper.createObjectNode();
        ObjectNode choice = responseBody.putArray("choices").addObject();
        choice.put("index", 0);
        choice.putObject("message").put("role", "assistant").put("content", fallbackContent);
        ResponseEntity<JsonNode> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(response);

        ModelFallbackHandler.ModelResult result = handler.callWithFallback(SYSTEM_PROMPT, USER_PROMPT);

        assertEquals(fallbackContent, result.getContent());
        assertEquals("qwen-turbo", result.getModelUsed());
        assertTrue(result.isFallbackActivated());

        // Only fallback model should have been called
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class));
    }

    // ======== Test: Primary API key blank, skips to fallback ========
    @Test
    void callWithFallback_primaryApiKeyBlank_skipsToFallback() {
        setupPrimaryConfig("   "); // Blank API key
        setupFallbackConfig("sk-test-fallback-key");
        handler = new ModelFallbackHandler(modelProperties, restTemplate);

        mockModelSuccess("Fallback result");

        ModelFallbackHandler.ModelResult result = handler.callWithFallback(SYSTEM_PROMPT, USER_PROMPT);

        assertEquals("qwen-turbo", result.getModelUsed());
        assertTrue(result.isFallbackActivated());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class));
    }
}
