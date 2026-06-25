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

浏览器打开 `http://localhost:8080`，在控制台输入告警文本后点击"开始分析"。

## API 接口

### POST /api/alarm/analyze

分析告警文本并返回结构化处置报告。

**请求**

```json
{
  "alarmText": "订单服务接口超时率达到15%，P99延迟超过5秒，影响用户下单"
}
```

**响应**

```json
{
  "analysisId": "uuid",
  "report": {
    "analysisStatus": "SUCCESS",
    "serviceName": "order-service",
    "alarmType": "接口超时",
    "riskLevel": "P1",
    "severity": "严重",
    "keyMetrics": { "超时率": "15%", "P99延迟": "5s" },
    "userImpact": true,
    "impactDescription": "影响用户下单",
    "possibleRootCause": "近期发布后数据库连接池耗尽导致接口超时",
    "evidence": ["[ServiceStatusTool] OK status=healthy (45ms)", "..."],
    "confidence": 0.85,
    "recommendedActions": ["建议回滚至v2.2.1", "调大超时阈值"],
    "shouldRollback": true,
    "needsEscalation": false,
    "followUpMetrics": ["接口P99延迟", "接口超时率", "服务健康状态"],
    "nextCheckTime": "持续监控，建议每5分钟检查一次"
  },
  "modelUsed": "deepseek-chat",
  "fallbackActivated": false,
  "pipelineLatency": {
    "parse": 1200,
    "toolInvocation": 500,
    "evidenceAggregate": 5,
    "rootCauseAnalysis": 3000,
    "reportGeneration": 20,
    "total": 5350
  },
  "toolResults": [
    {
      "toolName": "ServiceStatusTool",
      "serviceName": "order-service",
      "success": true,
      "data": "status=healthy",
      "latencyMs": 45,
      "errorMessage": null
    }
  ]
}
```

### GET /api/alarm/services

返回支持的服务列表和快捷告警示例。

## 关键行为

- 告警解析结果会经过服务白名单校验，仅支持 Mock 数据中的 `order-service`、`payment-service`、`inventory-service`、`user-service`。
- 未识别到有效服务或异常指标时，返回 `analysisStatus: PARSE_FAILED`，不再调用运维工具。
- 有效告警会固定调用服务状态、错误日志、发布记录、服务依赖、资源指标 5 个工具，报告证据来自工具结果。
- 主模型失败时自动切换备用模型；主备模型都不可用时使用规则引擎兜底。
- 回滚建议、人工升级、后续观察指标由独立规则生成，优先结合工具证据、风险等级和用户影响。

## 项目结构

```
alarm-assistant/
├── pom.xml                                  # Maven 配置
├── plan.md                                  # 项目计划
├── README.md                                # 本文件
├── AGENTS.md                                # Agent 编码约定
└── src/main/
    ├── java/com/example/alarm/
    │   ├── AlarmAssistantApplication.java   # 启动类
    │   ├── config/                          # 配置
    │   │   ├── ModelProperties.java         # 双模型配置属性
    │   │   └── LangChain4jConfig.java       # ChatModel Bean
    │   ├── model/                           # 模型
    │   │   ├── ParsedAlarm.java             # 告警解析结果
    │   │   ├── AlarmReport.java             # 结构化报告
    │   │   ├── ToolResult.java              # 工具调用结果
    │   │   └── mock/                        # Mock 实体
    │   ├── mock/
    │   │   └── OpsMockDataStore.java        # Mock 数据仓库
    │   ├── tools/                           # 5个运维工具
    │   │   ├── ServiceStatusTool.java
    │   │   ├── ErrorLogTool.java
    │   │   ├── DeployRecordTool.java
    │   │   ├── DependencyTool.java
    │   │   └── ResourceMetricsTool.java
    │   ├── pipeline/                        # 核心管线
    │   │   ├── AlarmAnalysisPipeline.java   # 五步分析编排
    │   │   └── ModelFallbackHandler.java    # 模型故障转移
    │   ├── controller/
    │   │   └── AlarmController.java         # REST API
    │   └── exception/                       # 异常处理
    │       ├── AlarmParseException.java
    │       ├── ToolInvocationException.java
    │       ├── ModelCallException.java
    │       └── GlobalExceptionHandler.java
    └── resources/
        ├── application.yml                  # 应用配置
        └── static/
            └── index.html                   # 运维控制台前端
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

`timeout` 会实际应用到模型 HTTP 客户端的连接超时和读取超时；如果 `api-key`、`base-url` 或 `model` 缺失，该模型会被跳过并进入备用模型或规则引擎兜底。

## 测试

```powershell
# 运行所有测试
E:\apache-maven-3.6.3\bin\mvn -f E:\geek\alarm-assistant\pom.xml clean test

# 查看覆盖率报告
# 浏览器打开: target/site/jacoco/index.html
```

**测试覆盖**

| 包 | 测试文件 | 覆盖 |
|---|---------|------|
| `tools` | 5 个 ToolTest | 100% |
| `exception` | GlobalExceptionHandlerTest | 100% |
| `mock` | OpsMockDataStoreTest | 97% |
| `model` | AlarmReportTest, ToolResultTest | 91% |
| `controller` | AlarmControllerTest | 84% |
| `config` | ModelFallbackHandlerTest | 82% |
| `pipeline` | AlarmAnalysisPipelineTest, ModelFallbackHandlerTest | 81% |
| **总计** | **12 个测试类 / 67 个用例** | **86%** |

新增覆盖：未知服务解析结果会短路为 `PARSE_FAILED`，模型调用会应用 `timeout` 配置。

## 技术栈

| 组件 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| LangChain4j | 1.0.0-beta4 |
| Maven | 3.6.3 |
