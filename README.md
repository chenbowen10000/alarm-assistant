# 企业运维告警智能处置助手

基于 Spring Boot 3.2.5 + LangChain4j 的多模型 AI 运维告警分析系统。支持自然语言告警文本输入，自动解析、调用运维工具、推理根因，输出结构化处置报告。

## 快速开始

### 1. 配置 API Key

```powershell
$env:DEEPSEEK_API_KEY = "sk-your-deepseek-key"
$env:QWEN_API_KEY = "sk-your-qwen-key"
```

至少配置一个模型的 Key。两个都不配置时，系统使用规则引擎兜底。

### 2. 构建

```powershell
E:\apache-maven-3.6.3\bin\mvn -f E:\geek\alarm-assistant\pom.xml clean package -DskipTests
```

### 3. 运行

```powershell
java -jar E:\geek\alarm-assistant\target\alarm-assistant-0.0.1-SNAPSHOT.jar
```

### 4. 访问

浏览器打开 `http://localhost:8080`，在控制台输入告警文本后点击"开始分析"。左侧面板提供快捷示例，点击即可填充。

## API 接口

### POST /api/alarm/analyze

分析告警文本并返回结构化处置报告。LLM 无法解析时返回 HTTP 400。

**请求**

```json
{
  "alarmText": "订单服务接口超时率达到15%，P99延迟超过5秒，影响用户下单"
}
```

**成功响应 (200)**

```json
{
  "analysisId": "uuid",
  "analysisStatus": "SUCCESS",
  "report": {
    "analysisStatus": "SUCCESS",
    "serviceName": "order-service",
    "alarmType": "接口超时",
    "riskLevel": "P1",
    "severity": "严重",
    "keyMetrics": { "超时率": "15%", "P99延迟": "5s" },
    "possibleRootCause": "v2.3.1发版引入的性能回归...",
    "recommendedActions": ["建议回滚至v2.2.1", "调大超时阈值"],
    "shouldRollback": true,
    "needsEscalation": true,
    "confidence": 0.85,
    "evidence": ["[ServiceStatusTool] ✓ status=healthy (45ms)", ...]
  },
  "modelUsed": "deepseek-chat",
  "fallbackActivated": false,
  "pipelineLatency": { "parse": 1200, "toolInvocation": 500, "total": 5350 },
  "toolResults": [...]
}
```

**解析失败响应 (400)**

```json
{
  "timestamp": "2026-06-07T18:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "大模型解析异常，请联系管理员",
  "alarmText": "这只是随便写写的异常"
}
```

### GET /api/alarm/services

返回支持的服务列表和快捷告警示例（动态加载到前端）。

## 分析管线

```
POST /api/alarm/analyze
  │
  ├─[Step 1] 告警解析 ─── LLM → ParsedAlarm
  │     ├─ serviceName 无效 / confidence < 0.3 → throw AlarmParseException → 400
  │     └─ LLM JSON 解析失败 → heuristicParse → 仍失败 → throw → 400
  ├─[Step 2] 工具调用 ─── 5 tools → List<ToolResult>
  ├─[Step 3] 证据汇总 ─── 上下文拼装
  ├─[Step 4] 根因分析 ─── LLM 推理
  └─[Step 5] 报告生成 ─── parseRiskLevel/parseRootCause/parseImpactDesc → AlarmReport
```

模型调用链：`主模型 (DeepSeek) → 备用模型 (Qwen) → 规则引擎兜底`

## 项目结构

```
alarm-assistant/
├── pom.xml                                  # Maven 配置
├── VersionChangelog.md                      # 版本变更记录
├── README.md                                # 本文件
├── AGENTS.md                                # Agent 编码约定
└── src/main/
    ├── java/com/example/alarm/
    │   ├── AlarmAssistantApplication.java   # 启动类
    │   ├── config/                          # 配置
    │   │   ├── AppConfig.java
    │   │   ├── ModelProperties.java
    │   │   └── LangChain4jConfig.java
    │   ├── model/                           # 模型
    │   │   ├── ParsedAlarm.java
    │   │   ├── AlarmReport.java
    │   │   ├── ToolResult.java
    │   │   └── mock/
    │   ├── mock/
    │   │   └── OpsMockDataStore.java
    │   ├── tools/                           # 5个运维工具
    │   │   ├── ServiceStatusTool.java
    │   │   ├── ErrorLogTool.java
    │   │   ├── DeployRecordTool.java
    │   │   ├── DependencyTool.java
    │   │   └── ResourceMetricsTool.java
    │   ├── pipeline/                        # 核心管线
    │   │   ├── AlarmAnalysisPipeline.java
    │   │   └── ModelFallbackHandler.java
    │   ├── controller/
    │   │   └── AlarmController.java
    │   └── exception/
    │       ├── AlarmParseException.java
    │       ├── ToolInvocationException.java
    │       ├── ModelCallException.java
    │       └── GlobalExceptionHandler.java
    └── resources/
        ├── application.yml
        └── static/
            └── index.html
```

## 模型配置

在 `application.yml` 中配置双模型参数：

| 参数 | 主模型 (DeepSeek) | 备用模型 (Qwen) |
|------|------------------|-----------------|
| base-url | `https://api.deepseek.com` | `https://dashscope.aliyuncs.com/compatible-mode` |
| model | `deepseek-chat` | `qwen-turbo` |
| temperature | 0.3 | 0.3 |
| max-tokens | 2000 | 2000 |
| timeout | 30s | 30s |

## 技术栈

| 组件 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| LangChain4j | 1.0.0-beta4 |
| Maven | 3.6.3 |

## 版本历史

详见 [VersionChangelog.md](./VersionChangelog.md)