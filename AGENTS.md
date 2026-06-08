# AGENTS.md

## 项目概述
企业运维告警智能处置助手（Alarm Assistant），Spring Boot 3.2.5 项目，基于 LangChain4j 实现多步骤告警分析管线。

## 技术栈
- Java 17, Spring Boot 3.2.5, Maven 3.6.3
- LangChain4j 1.0.0-beta4（`langchain4j-open-ai` 模块同时驱动 DeepSeek + Qwen）
- 前端：纯 HTML/CSS/JS (`src/main/resources/static/index.html`)

## 项目结构

| 层级 | 路径 | 职责 |
|------|------|------|
| 入口 | `AlarmAssistantApplication.java` | Spring Boot 启动类 |
| 配置 | `config/ModelProperties.java` | 双模型配置属性（primary + fallback） |
| 配置 | `config/LangChain4jConfig.java` | OpenAiChatModel Bean 注册 |
| 模型 | `model/ParsedAlarm.java` | LLM 解析后的告警结构化信息 |
| 模型 | `model/AlarmReport.java` | 最终结构化处置报告 |
| 模型 | `model/ToolResult.java` | 工具调用结果（含成功/失败/耗时） |
| Mock | `mock/OpsMockDataStore.java` | 4 服务 × 5 维度 Mock 数据 |
| 工具 | `tools/ServiceStatusTool.java` | 工具1: 查询服务状态 |
| 工具 | `tools/ErrorLogTool.java` | 工具2: 查询错误日志 |
| 工具 | `tools/DeployRecordTool.java` | 工具3: 查询发布记录 |
| 工具 | `tools/DependencyTool.java` | 工具4: 查询服务依赖 |
| 工具 | `tools/ResourceMetricsTool.java` | 工具5: 查询 CPU/内存/延迟/错误率 |
| 管线 | `pipeline/AlarmAnalysisPipeline.java` | 五步分析编排（解析→工具→证据→根因→报告） |
| 管线 | `pipeline/ModelFallbackHandler.java` | 模型故障转移（主→备→规则引擎） |
| 控制器 | `controller/AlarmController.java` | `POST /api/alarm/analyze` |
| 异常 | `exception/GlobalExceptionHandler.java` | 统一异常处理 |
| 测试 | `*/Test.java` | 12 个 JUnit 5 测试类（66 用例，86% 覆盖率） |

## 编码约定
- 构造器注入（不使用 `@Autowired`）
- 异常统一由 `@RestControllerAdvice` 处理
- 无 Lombok，手动 getter/setter
- 工具调用异常不阻断管线，返回 `ToolResult.error()`
- 模型调用使用 `ModelFallbackHandler.callWithFallback()`
- 每步记录结构化日志：`[STEP-N] id=xxx, ...`

## 模型配置
- 主模型: DeepSeek (`deepseek-chat`)，API Key 通过 `DEEPSEEK_API_KEY` 环境变量
- 备用模型: 通义千问 (`qwen-turbo`)，API Key 通过 `QWEN_API_KEY` 环境变量
- 模型配置在 `application.yml` 的 `alarm.models.primary/fallback` 下

## 运行命令
```powershell
E:\apache-maven-3.6.3\bin\mvn -f E:\geek\alarm-assistant\pom.xml clean package -DskipTests
java -jar target/alarm-assistant-0.0.1-SNAPSHOT.jar
```
