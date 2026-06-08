# 企业运维告警智能处置助手 — 项目计划与版本记录

## 项目计划（Plan）

基于 Spring Boot 3.2.5 + LangChain4j 1.0+ 的多模型 AI 运维告警分析系统。主模型 DeepSeek (`deepseek-chat`)，备用模型通义千问 (`qwen-turbo`)。输入自然语言告警文本，经五步分析管线输出结构化处置报告。

### 功能清单

| 功能 | 说明 |
|------|------|
| 告警文本识别 | LLM 提取服务名称、告警类型、异常指标、风险等级、用户影响 |
| Mock 运维数据 | 4 个服务（订单/支付/库存/用户）× 5 维度（状态/日志/发布/依赖/资源） |
| 多工具调用 | 5 个运维工具：服务状态、错误日志、发布记录、依赖查询、资源指标 |
| 多步骤分析管线 | 解析告警 → 调用工具 → 汇总证据 → 判断根因 → 生成建议 |
| 结构化处置报告 | 服务名、告警类型、风险等级、异常指标、根因、建议、回滚判断 |
| 多模型接入 | DeepSeek 主 + Qwen 备，配置化 temperature/max-tokens/timeout |
| 异常处理与日志 | 模型超时切换、工具失败不阻断、规则引擎兜底、全链路结构化日志 |

### 技术栈

- **后端**: Spring Boot 3.2.5, Java 17, Maven 3.6.3
- **AI**: LangChain4j 1.0.0-beta4 (`langchain4j-open-ai`)
- **前端**: 纯 HTML/CSS/JS，工业运维控制台风
- **无 Lombok**，手动 getter/setter，构造器注入

### 架构

```
POST /api/alarm/analyze
  │
  ├─[Step 1] 告警解析 ─── LLM → ParsedAlarm
  ├─[Step 2] 工具调用 ─── 5 tools → List<ToolResult>
  ├─[Step 3] 证据汇总 ─── 上下文拼装
  ├─[Step 4] 根因分析 ─── LLM 推理
  └─[Step 5] 报告生成 ─── → AlarmReport
```

模型调用链：`主模型 → 备用模型 → 规则引擎兜底`

### 构建与运行

```powershell
$env:DEEPSEEK_API_KEY = "sk-xxx"
$env:QWEN_API_KEY = "sk-xxx"
E:\apache-maven-3.6.3\bin\mvn -f E:\geek\alarm-assistant\pom.xml clean package -DskipTests
java -jar E:\geek\alarm-assistant\target\alarm-assistant-0.0.1-SNAPSHOT.jar
# 浏览器: http://localhost:8080
```

### 假设

- DeepSeek / Qwen 均通过 OpenAI 兼容接口调用
- 告警文本中英文混合，最终报告中文
- 规则引擎阈值为硬编码
- 无持久化存储、无用户认证

---

## 版本变更记录

### v1.0 — 初始版本 (2026-06-07)

- Spring Boot 3.2.5 + LangChain4j 项目骨架
- 5 个 Mock 运维工具（ServiceStatus / ErrorLog / DeployRecord / Dependency / ResourceMetrics）
- 五步告警分析管线（解析→工具→证据→根因→报告）
- 双模型接入（DeepSeek 主 + Qwen 备）+ ModelFallbackHandler
- 纯 HTML/CSS/JS 工业风前端

---

### v1.1 — favicon.ico 误报 ERROR 修复 (2026-06-07)

**问题**：浏览器自动请求 `/favicon.ico` 触发 Spring `NoResourceFoundException`，被 `GlobalExceptionHandler.handleGeneral(Exception.class)` 捕获并以 ERROR 级别打印完整堆栈，误导为系统故障。

**修复**：

| 文件 | 改动 |
|------|------|
| `exception/GlobalExceptionHandler.java` | 新增 `@ExceptionHandler(NoResourceFoundException.class)` — 静默返回 404，不记录日志 |

**效果**：favicon.ico 缺失不再刷 ERROR 日志。

---

### v1.2 — 快捷示例 API 化 (2026-06-07)

**问题**：前端快捷示例硬编码在 HTML 和 JS 中，增删示例需要同时改两处且需重新部署。

**修复**：

| 文件 | 改动 |
|------|------|
| `controller/AlarmController.java` | `listServices()` 新增 5 条示例（连接池耗尽、下游超时、GC 压力、重试风暴、配置变更） |
| `static/index.html` | 移除硬编码 `<button onclick="setExample(N)">`，改为 `<div id="quickExamplesContainer">` 动态容器 |
| `static/index.html` | 移除硬编码 `const examples = [...]` + `setExample(i)`，改为 `loadExamples()` → `fetch('/api/alarm/services')` → `renderExampleChips()` |
| `static/index.html` | API 不可用时降级为本地默认 4 条示例 |

**效果**：增删示例只需改后端 Controller，前端自动同步。

---

### v1.3 — 解析失败短路机制 (2026-06-07)

**问题**：输入无意义告警文本（如"这只是随便写写的异常"）时，LLM 仍编造分析结果，5 个工具全部查询无效数据，输出胡言乱语。

**修复**：

| 文件 | 改动 |
|------|------|
| `model/ParsedAlarm.java` | 新增 `parsed` 字段 + `ParsedAlarm.failed()` 工厂方法 |
| `model/AlarmReport.java` | 新增 `analysisStatus` 字段（`SUCCESS` / `PARSE_FAILED`） |
| `pipeline/AlarmAnalysisPipeline.java` | 新增 `hasAlarmSignal()` 预过滤 — 输入不含服务名/异常指标关键词直接拒绝，不调 LLM |
| `pipeline/AlarmAnalysisPipeline.java` | 新增 `isValidService()` 白名单校验 — 只接受 4 个已知服务名 |
| `pipeline/AlarmAnalysisPipeline.java` | 新增 `buildParseFailedResult()` — 短路返回结构化"无法解析"报告 |
| `pipeline/AlarmAnalysisPipeline.java` | `analyze()` Step 1 后加短路判断：`!parsedAlarm.isParsed()` → 跳过工具调用阶段 |
| `controller/AlarmController.java` | `buildReportMap()` 增加 `analysisStatus` 字段 |
| `static/index.html` | 新增 `PARSE_FAILED` 状态 UI — 展示 ⚠️ 警告卡片 + 建议 |

**效果**：无效输入 6ms 返回（不调 LLM、不调工具），`analysisStatus: PARSE_FAILED`。

---

### v1.4 — 风险等级定级修复 (2026-06-07)

**问题**：前端 UI 显示 `⚠ 应该是什么？ · 未知`、`⚠ 应该是什么？（P0/P1/P2/P3）** · 未知`。LLM 的 RCA 自然语言响应被 `extractFromRca` 当成风险等级原样提取，导致字段显示乱码。

**修复**：

| 文件 | 改动 |
|------|------|
| `pipeline/AlarmAnalysisPipeline.java` | 新增 `parseRiskLevel()` — 在 LLM 响应中扫描 `P0`-`P3` 关键字提取风险等级，不再用正则截取 |
| `pipeline/AlarmAnalysisPipeline.java` | 新增 `parseRootCause()` — 按"根因"关键词分割行提取，过滤 LLM 开场白废话 |
| `pipeline/AlarmAnalysisPipeline.java` | 新增 `parseImpactDesc()` — 提取用户影响描述 |
| `pipeline/AlarmAnalysisPipeline.java` | 新增 `isValidRiskLevel()` — 校验风险等级是否合法 |
| `pipeline/AlarmAnalysisPipeline.java` | `parseJsonResult()` 中加风险等级校验，LLM 返回非法值默认 `P2` |
| `pipeline/AlarmAnalysisPipeline.java` | `generateReport()` 改用 `parseRiskLevel()` / `parseRootCause()` / `parseImpactDesc()` |

**效果**：`riskLevel: P1, severity: 严重`，不再出现乱码。

---
### v1.5 — JUnit 5 单元测试 + JaCoCo 覆盖率 (2026-06-08)

**内容**：为全部 6 个包生成 12 个 JUnit 5 测试类，集成 JaCoCo 覆盖率插件，总覆盖率 86%。

**新增文件**：

| 文件 | 包 | 用例数 |
|------|-----|--------|
| `AlarmAnalysisPipelineTest.java` | `pipeline/` | 9 |
| `ModelFallbackHandlerTest.java` | `pipeline/` | 5 |
| `GlobalExceptionHandlerTest.java` | `exception/` | 7 |
| `AlarmControllerTest.java` | `controller/` | 4 |
| `OpsMockDataStoreTest.java` | `mock/` | 12 |
| `AlarmReportTest.java` | `model/` | 5 |
| `ToolResultTest.java` | `model/` | 4 |
| `ServiceStatusToolTest.java` | `tools/` | 3 |
| `ErrorLogToolTest.java` | `tools/` | 4 |
| `DeployRecordToolTest.java` | `tools/` | 3 |
| `DependencyToolTest.java` | `tools/` | 3 |
| `ResourceMetricsToolTest.java` | `tools/` | 3 |

**修改文件**：

| 文件 | 改动 |
|------|------|
| `pom.xml` | 新增 `spring-boot-starter-test` 依赖 + `jacoco-maven-plugin` 0.8.11 |

**覆盖率（JaCoCo）**：

| 包 | 指令 | 分支 |
|---|------|------|
| `tools` | 100% | 100% |
| `exception` | 100% | n/a |
| `mock` | 97% | 76% |
| `model` | 91% | 75% |
| `controller` | 84% | 83% |
| `config` | 82% | 100% |
| `pipeline` | 81% | 45% |
| **总计** | **86%** | **50%** |

**测试框架**：JUnit 5 + Mockito + AssertJ，`@WebMvcTest` 用于 Controller 层，`@ExtendWith(MockitoExtension.class)` 用于 Service 层。

---