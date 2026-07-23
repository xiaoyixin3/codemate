# 系统架构

## 总体结构

CodeMate 采用 Maven 多模块和分层架构：

```text
浏览器 / API 客户端
        |
        v
codemate-web
  Controller、认证拦截器、配置、全局异常处理、启动入口
        |
        +------> codemate-ui
        |        Thymeleaf 模板、JavaScript、CSS、静态资源
        |
        v
codemate-service
  文章、评论、用户、通知、搜索、Agent 等业务逻辑
        |
        v
codemate-core
  缓存、搜索、通用组件、工具、基础设施适配
        |
        v
codemate-api
  Entity、DTO、VO、枚举和公共接口模型
        |
        +------> MySQL / Redis / 外部模型服务
```

模块依赖方向保持由上到下，底层模块不依赖 Web 层。

## 模块职责

| 模块 | 职责 |
| --- | --- |
| `codemate-api` | Entity、DTO、VO、请求响应模型和枚举 |
| `codemate-core` | 通用工具、缓存、搜索、推荐和基础组件 |
| `codemate-service` | 业务服务、MyBatis-Plus 数据访问、事务编排 |
| `codemate-ui` | 服务端页面模板和静态资源 |
| `codemate-web` | Controller、REST 接口、认证、异常处理和应用入口 |

应用入口：

```text
codemate-web/src/main/java/com/github/paicoding/forum/web/QuickForumApplication.java
```

## 数据与状态

- MySQL：用户、文章、评论、关系、通知、Agent Run 等持久化数据。
- Redis：缓存、会话撤销和部分高频状态。
- Liquibase：数据库初始化和版本变更。
- `codemate-images` Docker Volume：用户上传图片。

默认数据库名继续使用 `pai_coding`。这是兼容性决策，不代表项目仍使用旧产品名。

## 认证模型

系统同时支持：

- 浏览器 Cookie 会话
- API 客户端 JWT Bearer Token

JWT 使用 HMAC 签名、过期时间校验以及 Redis 会话撤销校验。生产环境必须提供足够强度的 `JWT_SECRET`，禁止使用示例值。

## Agent 链路

```text
用户问题
  -> Agent 编排
  -> 会话记忆
  -> 站内知识检索 / 工具调用
  -> 模型生成
  -> 证据、步骤和运行轨迹持久化
  -> 工作台展示与失败重试
```

更详细的 Agent 与 RAG 设计位于 [`codemate-docs/docs`](../codemate-docs/docs/)。

## 兼容命名

当前已统一：

- 产品名：CodeMate
- Maven 模块与 artifactId：`codemate-*`
- 可执行 JAR：`codemate-web-0.0.1-SNAPSHOT.jar`

当前保留：

- Java 包名：`com.github.paicoding`
- 数据库名：`pai_coding`

若以后迁移包名或数据库名，应作为独立版本处理，并同时覆盖 Spring 扫描、MyBatis 映射、Liquibase、历史数据和服务器环境变量。
