package com.example.alarm.pipeline;

import com.example.alarm.config.ModelProperties;
import com.example.alarm.config.ModelProperties.ModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component
public class ModelFallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(ModelFallbackHandler.class);
    private final ModelProperties modelProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ModelFallbackHandler(ModelProperties modelProperties, RestTemplate restTemplate) {
        this.modelProperties = modelProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public static class ModelResult {
        private final String content;
        private final String modelUsed;
        private final boolean fallbackActivated;

        public ModelResult(String content, String modelUsed, boolean fallbackActivated) {
            this.content = content;
            this.modelUsed = modelUsed;
            this.fallbackActivated = fallbackActivated;
        }

        public String getContent() { return content; }
        public String getModelUsed() { return modelUsed; }
        public boolean isFallbackActivated() { return fallbackActivated; }
    }

    /**
     * Try primary -> fallback -> rule engine.
     */
    public ModelResult callWithFallback(String systemPrompt, String userPrompt) {
        // Try primary
        ModelConfig primary = modelProperties.getPrimary();
        if (primary.isValid()) {
            try {
                log.info("[MODEL] Trying primary model: {}", primary.getModel());
                long start = System.currentTimeMillis();
                String result = callModel(primary, systemPrompt, userPrompt);
                long latency = System.currentTimeMillis() - start;
                log.info("[MODEL] Primary model succeeded: {} ({}ms)", primary.getModel(), latency);
                return new ModelResult(result, primary.getModel(), false);
            } catch (Exception e) {
                log.warn("[FALLBACK] Primary model failed: {} - {}", primary.getModel(), e.getMessage());
            }
        } else {
            log.warn("[MODEL] Primary model API key not configured, skipping");
        }

        // Try fallback
        ModelConfig fallback = modelProperties.getFallback();
        if (fallback.isValid()) {
            try {
                log.info("[MODEL] Trying fallback model: {}", fallback.getModel());
                long start = System.currentTimeMillis();
                String result = callModel(fallback, systemPrompt, userPrompt);
                long latency = System.currentTimeMillis() - start;
                log.info("[MODEL] Fallback model succeeded: {} ({}ms)", fallback.getModel(), latency);
                return new ModelResult(result, fallback.getModel(), true);
            } catch (Exception e) {
                log.warn("[FALLBACK] Fallback model failed: {} - {}", fallback.getModel(), e.getMessage());
            }
        } else {
            log.warn("[MODEL] Fallback model API key not configured, skipping");
        }

        // Rule engine fallback
        log.warn("[FALLBACK] All models failed or not configured, using rule engine");
        String ruleResult = ruleEngineFallback(userPrompt);
        return new ModelResult(ruleResult, "rule-engine", true);
    }

    private String callModel(ModelConfig config, String systemPrompt, String userPrompt) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());

            ArrayNode messages = objectMapper.createArrayNode();

            ObjectNode sysMsg = objectMapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);

            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            requestBody.set("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            String url = config.getBaseUrl() + "/v1/chat/completions";
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            if (response.getBody() != null) {
                JsonNode choices = response.getBody().get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null && message.has("content")) {
                        return message.get("content").asText();
                    }
                }
            }
            throw new RuntimeException("模型返回结果为空");

        } catch (RestClientException e) {
            throw new RuntimeException("模型调用网络异常: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("模型调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * Simple rule engine: analyze key metrics from the prompt text to produce basic guidance.
     */
    private String ruleEngineFallback(String userPrompt) {
        StringBuilder sb = new StringBuilder();
        String lower = userPrompt.toLowerCase();

        String serviceName = "unknown";
        if (lower.contains("order") || lower.contains("订单")) serviceName = "order-service";
        else if (lower.contains("pay") || lower.contains("支付")) serviceName = "payment-service";
        else if (lower.contains("inventory") || lower.contains("库存")) serviceName = "inventory-service";
        else if (lower.contains("user") || lower.contains("用户")) serviceName = "user-service";

        String alarmType = "unknown";
        if (lower.contains("超时") || lower.contains("timeout") || lower.contains("延迟")) alarmType = "性能超时";
        else if (lower.contains("错误率") || lower.contains("error")) alarmType = "错误率上升";
        else if (lower.contains("cpu")) alarmType = "CPU资源异常";
        else if (lower.contains("内存") || lower.contains("memory")) alarmType = "内存资源异常";
        else if (lower.contains("数据库") || lower.contains("database") || lower.contains("连接池")) alarmType = "数据库异常";
        else if (lower.contains("发布") || lower.contains("deploy") || lower.contains("版本")) alarmType = "发布后异常";

        String riskLevel = "P2";
        if (lower.contains("全面") || lower.contains("所有") || lower.contains("宕机") || lower.contains("不可用")) riskLevel = "P0";
        else if (lower.contains("大量") || lower.contains("严重") || lower.contains("大面积")) riskLevel = "P1";
        else if (lower.contains("部分") || lower.contains("偶尔")) riskLevel = "P3";

        sb.append("### 规则引擎分析结果（模型不可用时的兜底分析）\n\n");
        sb.append("- 服务名称: ").append(serviceName).append("\n");
        sb.append("- 告警类型: ").append(alarmType).append("\n");
        sb.append("- 风险等级: ").append(riskLevel).append("\n");
        sb.append("- 用户影响: ").append(lower.contains("用户") || lower.contains("下单")).append("\n");
        sb.append("- 建议动作: 请运维人员根据上述指标人工核查，检查最近发布记录、资源使用和错误日志\n");
        sb.append("- 注意: 此分析由规则引擎生成（所有AI模型均不可用），准确性有限，请人工确认\n");
        return sb.toString();
    }
}
