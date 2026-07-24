# CodeMate 项目文档

本文档对应 CodeMate 当前 `main` 分支，用于开发、部署、运维和面试展示。

## 当前状态

- 项目类型：Spring Boot 多模块程序员社区与 AI Agent 平台
- Java 版本：17
- 核心存储：MySQL 8、Redis 7
- 数据库迁移：Liquibase
- 前端：Thymeleaf、JavaScript、CSS
- AI 能力：LangChain4j、流式模型调用、会话记忆、工具调用、站内知识检索、可靠性评估
- 构建与测试：Maven、GitHub Actions、Playwright
- 部署方式：Docker Compose；国内网络异常时可使用预构建 JAR 手动发布
- 当前演示地址：<http://43.136.86.251/>

> 当前地址仍为 HTTP IP 地址。正式对外使用前应绑定域名并启用 HTTPS。

## 文档导航

- [系统架构](architecture.md)
- [本地开发](local-development.md)
- [生产部署](production-deployment.md)
- [运维与故障排查](operations.md)
- [面试演示指南](interview-guide.md)

Agent、RAG、持久化记忆、模型可靠性和离线评估等深入设计，参见 [`codemate-docs/docs`](../codemate-docs/docs/)。

## 已完成

- Maven 模块和构建产物统一为 CodeMate：
  `codemate-api`、`codemate-core`、`codemate-service`、`codemate-ui`、`codemate-web`
- 保留兼容包名 `com.github.paicoding` 和数据库名 `pai_coding`，避免破坏历史数据与配置
- 完成 Cookie 与 JWT Bearer 登录、Redis 会话撤销校验
- 完成文章、评论、点赞、收藏、关注、通知、搜索和排行榜等社区能力
- 完成 Agent 工作台、站内知识检索、工具调用、会话记忆、降级与评估
- GitHub Actions 覆盖 Maven 测试、部署配置校验和 Playwright E2E
- 生产 JAR 已在腾讯云服务器完成部署，应用启动与本机 HTTP 检查通过

## 尚需完善

按优先级继续处理：

1. 修复 Dependabot 中的 Critical 和 High 依赖安全告警。
2. 配置域名、HTTPS、应用健康检查和失败自动回滚。
3. 将 MySQL、Redis 和用户图片备份改为定时任务，并执行恢复演练。
4. 增加 JVM、HTTP、容器、磁盘和业务错误监控。
5. 优化约 300 MB 的可执行 JAR 和镜像分层，降低发布传输成本。

这些事项不阻塞面试展示，但属于从“可以运行”走向“生产可维护”的必要工作。
