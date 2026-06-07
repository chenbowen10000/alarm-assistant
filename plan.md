# 企业运维告警智能处置助手 — 项目计划

## 摘要

基于 Spring Boot 3.2.5 + LangChain4j 1.0+ 的多模型 AI 运维告警分析系统。主模型 DeepSeek (`deepseek-chat`)，备用模型通义千问 (`qwen-turbo`)。输入自然语言告警文本，经五步分析管线输出结构化处置报告。

## 功能清单

| 功能 | 说明 |
|------|------|
| 告警文本识别 | LLM 提取服务名称、告警类型、异常指标、风险等级、用户影响 |
| Mock 运维数据 | 4 个服务（订单/支付/库存/用户）× 5 维度（状态/日志/发布/依赖/资源） |
| 多工具调用 | 5 个运维工具：服务状态、错误日志、发布记录、依赖查询、资源指标 |
| 多步骤分析管线 | 解析告警→调用工具→汇总证据→判断根因→生成建议 |
| 结构化处置报告 | 服务名、告警类型、风险等级、异常指标、根因、建议、回滚判断 |
| 多模型接入 | DeepSeek 主 + Qwen 备，配置化 temperature/max-tokens/timeout |
| 异常处理与日志 | 模型超时切换、工具失败不阻断、规则引擎兜底、全链路结构化日志 |

## 技术栈

- **后端**: Spring Boot 3.2.5, Java 17, Maven 3.6.3
- **AI**: LangChain4j 1.0.0-beta4 (`langchain4j-open-ai`)
- **前端**: 纯 HTML/CSS/JS，工业运维控制台风
- **无 Lombok**，手动 getter/setter，构造器注入

## 架构

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

## 构建与运行

```powershell
$env:DEEPSEEK_API_KEY = "sk-xxx"
$env:QWEN_API_KEY = "sk-xxx"
E:\apache-maven-3.6.3\bin\mvn -f E:\geek\alarm-assistant\pom.xml clean package -DskipTests
java -jar E:\geek\alarm-assistant\target\alarm-assistant-0.0.1-SNAPSHOT.jar
# 浏览器: http://localhost:8080
```

## 假设

- DeepSeek / Qwen 均通过 OpenAI 兼容接口调用
- 告警文本中英文混合，最终报告中文
- 规则引擎阈值为硬编码
- 无持久化存储、无用户认证
