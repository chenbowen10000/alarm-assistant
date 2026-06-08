package com.example.alarm.agent;

import com.example.alarm.model.ParsedAlarm;
import com.example.alarm.pipeline.ModelFallbackHandler;
import com.example.alarm.pipeline.ModelFallbackHandler.ModelResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ParserAgent {

    private static final Logger log = LoggerFactory.getLogger(ParserAgent.class);
    private final ModelFallbackHandler modelFallback;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT =
            "你是一个运维告警解析专家。请从自然语言告警文本中提取结构化信息。" +
            "只返回JSON，不要任何其他文字。格式如下：\n" +
            "{\"serviceName\":\"服务名\",\"alarmType\":\"告警类型\",\"keyMetrics\":{\"指标名\":\"值\"}," +
            "\"riskLevel\":\"P0/P1/P2/P3\",\"userImpact\":true/false,\"needsEscalation\":true/false,\"confidence\":0.0-1.0}\n\n" +
            "已知服务: order-service(订单服务), payment-service(支付服务), inventory-service(库存服务), user-service(用户服务)\n" +
            "告警类型: 接口超时, 错误率上升, CPU异常, 内存异常, 数据库异常, 发布后异常, 下游服务异常, 综合告警";

    public ParserAgent(ModelFallbackHandler modelFallback) {
        this.modelFallback = modelFallback;
        this.objectMapper = new ObjectMapper();
    }

    public ParsedAlarm parse(String alarmText, String contextPrompt) {
        String userPrompt = alarmText;
        if (contextPrompt != null && !contextPrompt.isBlank()) {
            userPrompt = contextPrompt + "\n" + alarmText;
        }

        ModelResult result = modelFallback.callWithFallback(SYSTEM_PROMPT, userPrompt);
        log.info("[AGENT-PARSER] model={}, fallback={}", result.getModelUsed(), result.isFallbackActivated());

        try {
            String json = extractJson(result.getContent());
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            ParsedAlarm alarm = new ParsedAlarm();
            alarm.setServiceName(getStr(map, "serviceName"));
            alarm.setAlarmType(getStr(map, "alarmType"));
            alarm.setRiskLevel(getStr(map, "riskLevel"));
            alarm.setUserImpact(getBool(map, "userImpact"));
            alarm.setNeedsEscalation(getBool(map, "needsEscalation"));
            alarm.setConfidence(getDbl(map, "confidence"));
            Object metrics = map.get("keyMetrics");
            if (metrics instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> m = (Map<String, String>) metrics;
                alarm.setKeyMetrics(m);
            if (alarm.getServiceName() == null || alarm.getServiceName().isBlank()) {
                return heuristicParse(alarmText);
            }
            }
            if (alarm.getServiceName() == null || alarm.getServiceName().isBlank()) {
                return heuristicParse(alarmText);
            }
            return alarm;
        } catch (Exception e) {
            log.warn("[AGENT-PARSER] Parse failed, using heuristic: {}", e.getMessage());
            return heuristicParse(alarmText);
        }
    }

    private ParsedAlarm heuristicParse(String text) {
        ParsedAlarm a = new ParsedAlarm();
        a.setConfidence(0.3);
        String lower = text.toLowerCase();
        if (lower.contains("order") || lower.contains("订单")) a.setServiceName("order-service");
        else if (lower.contains("pay") || lower.contains("支付")) a.setServiceName("payment-service");
        else if (lower.contains("inventory") || lower.contains("库存")) a.setServiceName("inventory-service");
        else if (lower.contains("user") || lower.contains("用户")) a.setServiceName("user-service");
        else a.setServiceName("unknown");
        if (lower.contains("超时") || lower.contains("timeout")) a.setAlarmType("接口超时");
        else if (lower.contains("错误") || lower.contains("error")) a.setAlarmType("错误率上升");
        else if (lower.contains("cpu")) a.setAlarmType("CPU异常");
        else a.setAlarmType("综合告警");
        a.setRiskLevel("P2");
        return a;
    }

    private String extractJson(String content) {
        int s = content.indexOf('{'), e = content.lastIndexOf('}');
        return (s >= 0 && e > s) ? content.substring(s, e + 1) : "{}";
    }

    private String getStr(Map<String, Object> m, String k) { Object v = m.get(k); return v != null ? v.toString() : ""; }
    private boolean getBool(Map<String, Object> m, String k) { Object v = m.get(k); return v instanceof Boolean ? (Boolean) v : "true".equalsIgnoreCase(String.valueOf(v)); }
    private double getDbl(Map<String, Object> m, String k) { Object v = m.get(k); try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; } }
}
