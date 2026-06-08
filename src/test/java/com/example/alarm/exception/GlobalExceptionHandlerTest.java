package com.example.alarm.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAlarmParseException_returns400() {
        AlarmParseException ex = new AlarmParseException("test alarm text", "invalid LLM output: svc=");

        ResponseEntity<Map<String, Object>> response = handler.handleAlarmParse(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("error")).isEqualTo("Bad Request");
        assertThat(response.getBody().get("message").toString()).contains("invalid LLM output");
        assertThat(response.getBody().get("alarmText")).isEqualTo("test alarm text");
    }

    @Test
    void handleAlarmParseException_hasTimestamp() {
        AlarmParseException ex = new AlarmParseException("bad text", "parse error");

        ResponseEntity<Map<String, Object>> response = handler.handleAlarmParse(ex);

        assertThat(response.getBody().get("timestamp")).isNotNull();
    }

    @Test
    void handleToolInvocationException_returns500() {
        ToolInvocationException ex = new ToolInvocationException("ServiceStatusTool", "DB connection failed");

        ResponseEntity<Map<String, Object>> response = handler.handleToolInvocation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(500);
        assertThat(response.getBody().get("error")).isEqualTo("Internal Server Error");
        assertThat(response.getBody().get("message").toString()).contains("ServiceStatusTool");
    }

    @Test
    void handleModelCallException_returns500() {
        ModelCallException ex = new ModelCallException("deepseek-chat", "Connection refused");

        ResponseEntity<Map<String, Object>> response = handler.handleModelCall(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(500);
        assertThat(response.getBody().get("message").toString()).contains("deepseek-chat");
    }

    @Test
    void handleNoResourceFound_returns404() {
        org.springframework.web.servlet.resource.NoResourceFoundException ex =
                new org.springframework.web.servlet.resource.NoResourceFoundException(null, "favicon.ico");

        ResponseEntity<Void> response = handler.handleNoResourceFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleGeneralException_returns500() {
        Exception ex = new RuntimeException("Unexpected runtime failure");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(500);
        assertThat(response.getBody().get("message").toString()).contains("Unexpected runtime failure");
    }

    @Test
    void handleGeneralException_hasTimestamp() {
        Exception ex = new Exception("test");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getBody().get("timestamp")).isNotNull();
    }
}