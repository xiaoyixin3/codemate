# CodeMate 技术社区后端系统

CodeMate 是一个基于 Spring Boot 的程序员技术社区后端系统，支持用户登录注册、文章发布、评论回复、点赞收藏、用户关注、消息通知、内容搜索和排行榜等功能。

本项目由开源社区项目 Paicoding 二次开发而来。目前对外统一使用 CodeMate 名称，内部 Maven 模块、Java 包名和数据库名暂时保留原有命名，以保证改造过程稳定可运行。

## 项目定位

- 面向程序员技术交流场景，覆盖内容生产、内容互动、用户关系和消息通知等核心业务。
- 采用分层、模块化的后端结构，便于理解完整社区系统的请求处理流程。
- 作为个人后端简历项目，重点展示 Spring Boot、MySQL、Redis、Liquibase 和业务模块设计能力。

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot、Spring MVC |
| 数据访问 | MyBatis、MyBatis-Plus |
| 数据存储 | MySQL、Redis |
| 数据库迁移 | Liquibase |
| 工程构建 | Maven |
| 通用能力 | AOP、拦截器、定时任务、全局异常处理 |
| 接口风格 | RESTful API |

> 系统同时支持浏览器 Cookie 登录和面向 API 客户端的 JWT Bearer 登录。JWT 采用 HMAC 签名、过期校验与 Redis 会话撤销校验；生产环境必须配置 `JWT_SECRET`。

## 核心功能

- 用户模块：注册登录、个人资料、关注与粉丝关系。
- 文章模块：文章发布、编辑、删除、详情和分类标签查询。
- 互动模块：评论回复、点赞、收藏和阅读记录。
- 通知模块：评论、点赞、关注等行为触发站内消息。
- 搜索模块：关键词提示和文章内容检索。
- 排行榜模块：按时间范围统计和展示热门内容。
- 系统基础能力：统一响应、异常处理、权限校验、数据库初始化和配置管理。

## 项目结构

```text
paicoding
├── paicoding-api       DTO、VO、请求参数和响应模型
├── paicoding-core      通用工具、缓存、异常、枚举等基础能力
├── paicoding-service   文章、评论、用户、通知、搜索等业务逻辑
├── paicoding-web       Controller、拦截器、配置类和应用启动入口
├── paicoding-ui        页面模板与前端静态资源
└── codemate-docs       CodeMate 个人项目文档
```

典型后端请求流程：

```text
客户端请求
  -> Controller
  -> Service
  -> Repository / Mapper
  -> MySQL / Redis
  -> 统一响应结果
```

## 本地运行

### 环境要求

- JDK 17
- Maven 3.x
- MySQL 8.x
- Redis

### 启动步骤

1. 创建 MySQL 数据库：

   ```sql
   CREATE DATABASE pai_coding DEFAULT CHARACTER SET utf8mb4;
   ```

2. 在本地修改开发环境配置：

   ```text
   paicoding-web/src/main/resources-env/dev/application-dal.yml
   ```

   填写本机 MySQL 和 Redis 连接信息。该文件可能包含本地密码，请勿提交到版本库。

3. 启动 Redis。

4. 运行启动类：

   ```text
   paicoding-web/src/main/java/com/github/paicoding/forum/web/QuickForumApplication.java
   ```

首次启动时，Liquibase 会执行数据库变更脚本，初始化表结构和基础数据。

## 项目文档

- [项目介绍](codemate-docs/docs/01-项目介绍.md)
- [模块说明](codemate-docs/docs/02-CodeMate-modules.md)
- [数据库设计](codemate-docs/docs/03-CodeMate-database.md)
- [核心接口](codemate-docs/docs/04-CodeMate-api.md)
- [个人化配置清单](codemate-docs/docs/05-CodeMate-personalization.md)
- [Agent 与站内知识 RAG](codemate-docs/docs/06-CodeMate-agent-rag.md)
- [LangChain4j Agent 实战说明](codemate-docs/docs/07-CodeMate-LangChain4j-Agent.md)
- [持久化会话记忆](codemate-docs/docs/08-CodeMate-persistent-memory.md)
- [模型抽象与可靠性](codemate-docs/docs/09-CodeMate-model-reliability.md)
- [Agent 离线评估](codemate-docs/docs/10-CodeMate-agent-evaluation.md)
- [Agent 工作台](codemate-docs/docs/11-CodeMate-agent-workbench.md)
- [Agent 可复现演示](codemate-docs/docs/12-CodeMate-reproducible-demo.md)
- [面试演示与答辩指南](codemate-docs/docs/13-CodeMate-interview-guide.md)

### Agent 演示预检

配置当前终端的 `DB_USERNAME`、`DB_PASSWORD` 后，可运行不依赖付费模型的完整闭环验收：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run-codemate-demo.ps1
```

该场景覆盖站内文章检索、证据记录、Bug 诊断确认、修复计划创建、步骤执行结果和 Agent Run 轨迹，并在结束后自动清理隔离的演示数据。

### Agent 工作台前端验收

安装 Node.js 22 后，可运行不依赖后端服务和付费模型的 Playwright 交互测试：

```bash
npm ci
npx playwright install chromium
npm run test:e2e
```

测试直接加载生产环境的 Agent 工作台 JavaScript 和 CSS，覆盖 Run 详情与证据展示、敏感参数隐藏、历史切换、失败重试、取消、移动端抽屉和接口错误提示。

## 已完成改造

- 新增 CodeMate 项目介绍、模块、数据库和接口文档。
- 修复 MySQL 空库首次启动时 `DATABASECHANGELOG` 表不存在导致的初始化失败问题。
- 梳理核心业务模块、数据库表关系和主要接口。
- 将项目对外名称和简历描述统一为 CodeMate。
- 接入 LangChain4j、DeepSeek 流式 AI Services、会话记忆、工具调用、站内知识 RAG、降级与 Agent 指标。

## 后续计划

- 补充刷新令牌、多端会话管理与登录审计能力。
- 使用 Redis 缓存热点文章详情和高频用户信息。
- 基于 Redis ZSet 实现文章热度排行榜。
- 通过定时任务同步或重建排行榜统计数据。
- 优化站内消息通知，完善未读消息计数。
- 增加接口限流、操作日志和相应测试。
- 已完成页面标题、Logo 文案和生产环境站点名称的 CodeMate 品牌替换。

## 简历描述

基于 Spring Boot 搭建的程序员技术社区后端系统，支持用户登录注册、文章发布、评论回复、点赞收藏、用户关注、消息通知、内容搜索和排行榜等功能。项目采用 MySQL 存储核心业务数据，使用 Redis 支撑缓存和统计场景，并通过 Liquibase 管理数据库结构初始化与版本变更。

## 改造说明

当前阶段以业务梳理、文档建设和后端能力增强为主：

```text
项目对外名称：CodeMate
内部模块与包名：暂时保留 paicoding
当前数据库名称：pai_coding
```

工程模块、Java 包名和数据库的整体重命名会涉及 Maven 依赖、Spring 扫描、MyBatis 映射和配置文件，暂不作为当前阶段的必要改造。

构建项目显示名与 Spring 应用名已调整为 CodeMate；Maven artifactId 仍保持 `paicoding-forum`，以兼容现有模块依赖与部署脚本。
