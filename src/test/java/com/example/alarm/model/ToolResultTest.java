package com.example.alarm.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ToolResultTest {

    @Test
    void success_shouldSetAllFields() {
        ToolResult r = ToolResult.success("ToolA", "svc1", "data123", 42L);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getToolName()).isEqualTo("ToolA");
        assertThat(r.getServiceName()).isEqualTo("svc1");
        assertThat(r.getData()).isEqualTo("data123");
        assertThat(r.getLatencyMs()).isEqualTo(42L);
        assertThat(r.getErrorMessage()).isNull();
    }

    @Test
    void error_shouldSetAllFields() {
        ToolResult r = ToolResult.error("ToolB", "svc2", "boom");
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getToolName()).isEqualTo("ToolB");
        assertThat(r.getServiceName()).isEqualTo("svc2");
        assertThat(r.getErrorMessage()).isEqualTo("boom");
        assertThat(r.getData()).isEqualTo("null");
    }

    @Test
    void toString_success_shouldContainData() {
        ToolResult r = ToolResult.success("T", "s", "hello", 10L);
        assertThat(r.toString()).contains("T").contains("hello").contains("10ms");
    }

    @Test
    void toString_error_shouldContainErrorMessage() {
        ToolResult r = ToolResult.error("T", "s", "fail");
        assertThat(r.toString()).contains("FAIL").contains("fail");
    }
}