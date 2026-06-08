package com.example.alarm.agent;

import com.example.alarm.config.ModelProperties;
import com.example.alarm.config.ModelProperties.ModelConfig;
import com.example.alarm.model.ParsedAlarm;
import com.example.alarm.pipeline.ModelFallbackHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class ParserAgentTest {

    private ParserAgent parserAgent;

    @BeforeEach
    void setUp() {
        ModelProperties props = new ModelProperties();
        ModelConfig cfg = new ModelConfig();
        cfg.setApiKey("");
        cfg.setProvider("test");
        cfg.setBaseUrl("https://test.com");
        cfg.setModel("test");
        props.setPrimary(cfg);
        props.setFallback(cfg);
        ModelFallbackHandler fallback = new ModelFallbackHandler(props, new RestTemplate());
        parserAgent = new ParserAgent(fallback);
    }

    @Test
    void testHeuristicParseOrderService() {
        ParsedAlarm result = parserAgent.parse("order-service timeout, CPU 100 percent", "");
        assertNotNull(result);
        assertTrue(result.getServiceName().contains("order"), "Should detect order service");
        assertFalse(result.getAlarmType().isBlank(), "Should have alarm type");
    }

    @Test
    void testHeuristicParsePaymentService() {
        ParsedAlarm result = parserAgent.parse("payment service error rate high", "");
        assertNotNull(result);
        assertTrue(result.getServiceName().contains("payment"), "Should detect payment service");
    }
}
