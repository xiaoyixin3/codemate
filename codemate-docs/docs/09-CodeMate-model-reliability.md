# CodeMate 模型抽象与可靠性

## 目标

第六阶段将 Agent 业务编排与 DeepSeek 的具体实现解耦。业务层只依赖 `ChatModelProvider`，Provider 负责暴露模型名称、流式输出、工具调用、结构化输出和上下文窗口能力，并提供 LangChain4j 的同步与流式模型实例。

当前内置两个 Provider：

- `deepseek`：保留现有 DeepSeek OpenAI 兼容接口；
- `openai-compatible`：接入任意实现 OpenAI Chat Completions 协议的服务。

两者都复用 LangChain4j 1.15.1 的 OpenAI 客户端。模型输出 Token 上限会同时受 `AGENT_MAX_RUN_TOKEN_BUDGET` 和 Provider 上下文窗口约束。

## 配置

默认使用 DeepSeek：

```text
AGENT_MODEL_PROVIDER=deepseek
DEEPSEEK_API_HOST=https://api.deepseek.com/v1
DEEPSEEK_API_KEY=通过环境变量注入
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_CONTEXT_WINDOW_TOKENS=64000
```

切换到通用 OpenAI 兼容服务：

```text
AGENT_MODEL_PROVIDER=openai-compatible
OPENAI_COMPATIBLE_BASE_URL=https://api.openai.com/v1
OPENAI_COMPATIBLE_API_KEY=通过环境变量注入
OPENAI_COMPATIBLE_MODEL=gpt-4o-mini
OPENAI_COMPATIBLE_CONTEXT_WINDOW_TOKENS=128000
```

可靠性参数：

```text
AGENT_TIMEOUT_SECONDS=120
AGENT_FIRST_TOKEN_TIMEOUT_SECONDS=20
AGENT_TOTAL_RESPONSE_TIMEOUT_SECONDS=120
AGENT_MAX_RUN_TOKEN_BUDGET=16000
LANGCHAIN4J_FALLBACK_ENABLED=true
```

`AGENT_TIMEOUT_SECONDS` 控制底层 HTTP 请求；首 Token 超时控制用户首次得到流式内容的最长等待；总响应超时从原始请求开始计时，降级尝试共享同一预算，避免多次降级无限延长请求。

## 错误分类与降级

模型错误统一分为：

- `RETRIABLE_NETWORK`：连接中断、超时和可重试服务错误；
- `RATE_LIMIT`：HTTP 429 或 Provider 限流；
- `MODEL`：模型返回异常、协议错误或不可识别错误；
- `BUSINESS`：参数、工具参数或业务规则校验失败。

网络、限流或模型类流式调用失败后按以下顺序降级；业务校验错误直接返回，不进行无意义的模型降级：

```text
Agent（工具与业务编排） → KNOWLEDGE_QA（站内 RAG） → CHAT（普通问答）
```

每次降级都会在流式回答中插入明确提示。三级链路共享总响应超时；已失败尝试的迟到回调会被原子终态拦截，不能再次结束响应。降级链路只注册只读工具，写工具不会因模型错误、重试或降级而重复执行；实际写操作仍要求后端可信用户上下文和持久化幂等键。

如果 LangChain4j Provider 在请求开始前不可用，现有兼容链路仍可降级到普通问答，并明确说明该回答不包含 Agent 工具执行结果。

## 指标

Micrometer 指标包括：

- `codemate.agent.requests`：请求量，标签包含模式和 Provider；
- `codemate.agent.success`：成功量；
- `codemate.agent.errors`：失败量，包含 `type` 错误分类；
- `codemate.agent.first_token`：首 Token 耗时；
- `codemate.agent.duration`：单次模型尝试总耗时；
- `codemate.agent.tokens`：Token 用量；
- `codemate.agent.fallbacks`：降级次数和起止模式；
- `codemate.agent.tools`：工具调用结果；
- `codemate.agent.rag.retrieved`：RAG 命中分块数。

原有 Tool 与 RAG 细分指标继续保留。

## 验证

默认测试不调用外部付费模型：

```bash
mvn -pl paicoding-service -am "-Dtest=ChatModelProviderRegistryTest,ModelFailureClassifierTest,AgentFallbackPolicyTest,StreamTimeoutPolicyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl paicoding-service -am test
mvn clean install -DskipTests=true
```

Provider 切换的人工冒烟测试只需更换上述环境变量并重启服务；Agent Run 的 `model` 字段会记录 `provider:model`，便于对照指标和故障记录。
