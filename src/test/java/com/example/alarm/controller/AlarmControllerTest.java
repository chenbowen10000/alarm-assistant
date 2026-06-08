package com.example.alarm.controller;

import com.example.alarm.model.AlarmReport;
import com.example.alarm.pipeline.AlarmAnalysisPipeline;
import com.example.alarm.pipeline.AlarmAnalysisPipeline.PipelineResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlarmController.class)
class AlarmControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AlarmAnalysisPipeline pipeline;

    @Test
    void analyze_validAlarm_returns200() throws Exception {
        AlarmReport report = new AlarmReport();
        report.setAnalysisStatus("SUCCESS");
        report.setServiceName("order-service");
        report.setAlarmType("接口超时");
        report.setRiskLevel("P1");
        report.setSeverity("严重");

        Map<String, Long> lat = new LinkedHashMap<>();
        lat.put("parse", 100L); lat.put("total", 500L);

        PipelineResult result = new PipelineResult("test-id", report, "deepseek-chat", false, lat, Collections.emptyList());
        when(pipeline.analyze(anyString())).thenReturn(result);

        mockMvc.perform(post("/api/alarm/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"alarmText\":\"订单服务超时\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value("test-id"))
                .andExpect(jsonPath("$.report.serviceName").value("order-service"))
                .andExpect(jsonPath("$.report.riskLevel").value("P1"));
    }

    @Test
    void analyze_emptyAlarmText_returns400() throws Exception {
        mockMvc.perform(post("/api/alarm/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"alarmText\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyze_nullAlarmText_returns400() throws Exception {
        mockMvc.perform(post("/api/alarm/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listServices_returnsServicesAndExamples() throws Exception {
        mockMvc.perform(get("/api/alarm/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services").isArray())
                .andExpect(jsonPath("$.examples").isArray());
    }
}