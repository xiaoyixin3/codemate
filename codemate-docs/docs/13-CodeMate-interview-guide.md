# CodeMate 面试演示与答辩指南

## 一分钟项目介绍

CodeMate 是一个从技术社区项目演进而来的 Java AI Agent 平台。项目不是在页面上简单调用一次大模型，而是把模型抽象、流式输出、会话记忆、安全工具调用、站内知识 RAG、结构化任务规划、Bug 诊断确认、执行轨迹、可靠性降级和离线评测放进同一套可测试的业务闭环。

核心技术栈为 Java 17、Spring Boot 2.7、LangChain4j、DeepSeek/OpenAI 兼容模型、MySQL、MyBatis-Plus、Redis、Liquibase、Micrometer、Thymeleaf 和 Playwright。

## 三分钟架构讲解

```text
聊天页面
  → 请求解析与模式路由
  → Agent 编排器
      ├─ ChatMemory：按用户、会话和模式隔离
      ├─ Tool Calling：只读工具、可信用户绑定、限时限长
      ├─ RAG：文章切块、Embedding、混合检索、Top-K 证据
      ├─ Model Provider：DeepSeek / OpenAI Compatible
      └─ Reliability：超时、错误分类、降级、终态保护
  → Agent Run / Step / Evidence 持久化
  → 流式回答与工作台展示
```

介绍时优先讲三个设计原则：

1. **安全边界在后端**：模型不能传 `userId`；任务工具只使用服务端认证绑定，写操作必须用户确认。
2. **每次执行可解释**：Run 记录状态、预算和模型，Step 记录脱敏参数摘要与结果，Evidence 记录引用来源。
3. **结果可以重复验证**：确定性演示、离线评测和浏览器 E2E 默认都不调用付费模型。

## 十分钟演示顺序

### 1. 自动闭环

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '<本机数据库密码>'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run-codemate-demo.ps1
```

展示 `BUILD SUCCESS`，说明自动场景覆盖文章检索、证据、Bug 诊断、二次确认、修复计划、步骤状态机、审计、Run 终态及跨用户越权拦截，并会清理隔离数据。

### 2. Agent 工作台

登录后打开 `/chat`：

1. 选择“知识问答”，展示站内文章引用；
2. 打开 Agent 工作台，展示 Run、工具步骤、Token 预算和证据；
3. 切换“Bug 诊断”，展示原因假设、置信度、验证与回归方案；
4. 保存为修复计划，说明确认、事务和幂等设计；
5. 展示失败 Run 的错误原因、重试和取消能力；
6. 缩窄浏览器窗口，展示移动端工作台抽屉。

### 3. 工程质量

打开 GitHub Actions，展示两个独立检查：

- Maven `test`：后端、数据库迁移与 Agent 业务测试；
- `agent-workbench-e2e`：生产工作台 JavaScript/CSS 的 Chromium 交互测试。

## 高频问题与回答要点

### 为什么使用 LangChain4j，而不是手写 Prompt 调用？

AI Services 能统一模型、记忆、工具和 RAG 的编排边界。项目仍是 Spring Boot 2.7，所以选择 Plain Java API 手动装配，避免为了 Starter 一次性迁移到 Jakarta；这是兼顾能力升级和迁移风险的取舍。

### 为什么向量先存在 MySQL？

当前文章规模可控，MySQL 能减少部署组件并保持文章和索引的事务边界。检索层通过接口隔离，规模增长后可以迁移到 pgvector、Milvus 或 Elasticsearch，而不修改上层 Agent 编排。

### 如何避免不同会话串话？

记忆键由用户、会话和模式共同组成；同一记忆键通过原子闸门限制并发流式任务。异步回调只能完成一次终态，迟到回调不能覆盖已经失败或取消的 Run。

### 工具调用如何保证安全？

工具按风险分级，当前自动开放的工具均为只读。参数统一校验、限长、超时并转换为稳定错误；日志和数据库只保存参数哈希或脱敏摘要。涉及本人任务的工具不接收模型提供的用户 ID。

### 写操作如何避免重复执行？

Bug 诊断先进入 `WAITING_CONFIRMATION`。确认时锁定诊断和计划归属，在事务内创建计划、步骤与审计，并使用唯一幂等键抵御重复提交和并发重试。

### 模型或网络失败怎么办？

错误分为网络、限流、模型和业务错误。可重试错误按 Agent、知识问答、普通聊天降级，并共享总超时预算；业务校验错误不会盲目重试，写工具也不会因降级重复执行。

### 如何评估 Agent，而不是凭感觉判断？

离线评测覆盖 RAG Recall/MRR、引用合法率、无证据拒答率、规划完整性、工具选择与参数合法性、重复调用和越权拦截、Bug 诊断完整率。固定样本与随机种子保证版本可比较，外部模型评测默认关闭。

## 可以主动说明的边界

- 当前 RAG 使用 MySQL 候选集余弦计算，适合中小规模数据，不宣称大规模向量检索性能。
- 离线样本中的分数用于验证评测器能发现回归，不代表线上模型的真实质量。
- 自动演示验证业务闭环；真实模型效果仍需要独立数据集、线上指标和人工抽检。
- GHCR 镜像可以重复发布，但公开网站部署需要个人服务器、域名、TLS 和生产密钥。

主动说明边界比夸大“生产级”更可信，也便于引出后续演进方案。

## 简历描述

> 基于 Java 17、Spring Boot 与 LangChain4j 将技术社区 AI 能力升级为多模式 Agent，接入流式模型、会话级记忆、安全 Tool Calling、站内文章混合 RAG、Bug 诊断到任务计划状态机；设计 Run/Step/Evidence 执行轨迹、超时降级、幂等与越权保护，并通过离线评测、确定性集成测试、Playwright E2E 和 GitHub Actions 建立可重复验收链路。

## 面试前检查清单

- `main` 分支 Actions 全部通过；
- 自动演示命令执行成功；
- 演示数据库存在至少一篇匹配文章；
- 模型密钥只通过环境变量注入；
- 浏览器已登录，`/chat` 与 `/task-plan` 可访问；
- 准备一个成功 Run、一个失败 Run 和一个等待确认的 Bug 诊断；
- 不展示密码、Token、Cookie、完整工具参数或生产日志；
- 能清楚说明项目边界和下一步演进方案。
