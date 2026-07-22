# CodeMate 核心接口说明

## Agent Run 只读轨迹接口

以下接口要求登录，后端始终使用可信会话用户过滤数据，不接受客户端传入 `userId`：

| 方法 | 地址 | 说明 |
|---|---|---|
| GET | `/agent-run/api?limit=20` | 查询当前用户最近的 Agent Run |
| GET | `/agent-run/api/{runId}` | 查询 Run、工具步骤和 RAG 证据详情 |
| PUT | `/agent-run/api/{runId}/cancel` | 将当前用户拥有的非终态 Run 标记为取消 |

详情响应包含状态、模型、Token、工具调用次数、配置限额、失败原因、步骤和证据。其他用户的 Run 会统一按不存在或无权访问处理。

## 一、接口文档定位

本文档用于梳理 CodeMate 技术社区后端系统的核心接口能力，方便后续编写 README、简历描述和面试讲解稿。

当前项目仍然保留原 Paicoding 工程结构，接口路径也暂时沿用原系统设计。后续如果要改造成更标准的纯后端项目，可以在保持业务逻辑不变的前提下，把页面渲染型接口逐步整理为 RESTful JSON 接口。

## 二、统一响应结构

项目接口统一使用 `ResVo<T>` 返回结果，常见结构可以理解为：

```json
{
  "status": 0,
  "data": {},
  "msg": "ok"
}
```

接口成功时返回 `ResVo.ok(data)`，失败时返回 `ResVo.fail(...)`。这种统一响应结构方便前端统一处理成功、失败、错误提示和登录失效等情况。

## 三、认证与权限说明

系统同时提供浏览器兼容的 Cookie 登录和面向 API 客户端的 JWT Bearer 登录。JWT 由服务端签名，包含过期时间，并通过 Redis 记录实现主动注销；部分接口使用 `@Permission(role = UserRole.LOGIN)` 标记，表示需要登录后才能访问。

浏览器登录流程：

```text
用户提交用户名和密码
        ↓
LoginService 校验账号密码
        ↓
生成 session 标识
        ↓
写入 Cookie
        ↓
后续请求通过 Cookie 识别当前用户
```

API 客户端登录流程：

```text
登录成功后签发 JWT
        ↓
前端请求携带 Authorization: Bearer token
        ↓
拦截器解析 token
        ↓
写入用户上下文 ReqInfoContext
        ↓
Controller / Service 获取当前用户 ID
```

面试中可以说明：在保留浏览器 Cookie 兼容性的同时，补齐了 JWT Bearer 登录、统一请求上下文注入与 Redis 主动注销机制，以支持前后端分离客户端。

## 四、用户认证接口

对应 Controller：

```text
codemate-web/src/main/java/com/github/paicoding/forum/web/front/login/pwd/LoginRestController.java
```

| 接口 | 方法 | 是否登录 | 说明 |
| --- | --- | --- | --- |
| `/login/username` | POST | 否 | 用户名密码登录 |
| `/login/register` | POST | 否 | 注册账号并登录 |
| `/logout` | GET/POST | 是 | 退出登录 |
| `/api/auth/login` | POST | 否 | API 客户端密码登录并获取 JWT |
| `/api/auth/me` | GET | 是 | 查询当前 Bearer Token 对应用户 |
| `/api/auth/logout` | POST | 是 | 撤销当前 Bearer Token |

### API 客户端 JWT 登录

```text
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "alice",
  "password": "secret"
}
```

成功后返回 `accessToken`、`tokenType`（固定为 `Bearer`）和 `expiresIn`。后续请求应携带：

```text
Authorization: Bearer <accessToken>
```

`POST /api/auth/logout` 会撤销当前 Token 在 Redis 中的有效记录；已注销或过期 Token 不能继续访问需要登录权限的接口。

### 1. 用户名密码登录

```text
POST /login/username
```

请求参数：

| 参数 | 说明 |
| --- | --- |
| `username` | 用户名 |
| `password` | 密码 |

处理逻辑：

1. 调用 `LoginService.loginByUserPwd` 校验用户名和密码。
2. 登录成功后生成 session。
3. 将 session 写入 Cookie。
4. 返回登录成功结果。

### 2. 注册账号

```text
POST /login/register
```

处理逻辑：

1. 接收用户注册参数。
2. 调用 `LoginService.registerByUserPwd` 完成注册。
3. 注册成功后写入 Cookie。
4. 返回当前登录用户 ID。

### 3. 退出登录

```text
/logout
```

处理逻辑：

1. 清理服务端 session。
2. 调用登录服务执行退出逻辑。
3. 删除登录 Cookie。
4. 重定向回来源页面。

## 五、文章接口

对应 Controller：

```text
codemate-web/src/main/java/com/github/paicoding/forum/web/front/article/rest/ArticleRestController.java
codemate-web/src/main/java/com/github/paicoding/forum/web/front/article/rest/ArticleListRestController.java
```

| 接口 | 方法 | 是否登录 | 说明 |
| --- | --- | --- | --- |
| `/article/api/data/detail/{articleId}` | GET | 否 | 查询文章详情 |
| `/article/api/post` | POST | 是 | 发布或保存文章 |
| `/article/api/delete` | GET/POST | 是 | 删除文章 |
| `/article/api/favor` | GET | 是 | 收藏、点赞文章 |
| `/article/api/recommend` | GET/POST | 否 | 查询文章相关推荐 |
| `/article/api/tag/list` | GET | 否 | 查询标签列表 |
| `/article/api/category/list` | GET | 否 | 查询分类列表 |
| `/article/api/list/data/category/{category}` | GET | 否 | 查询分类文章列表 |
| `/article/api/list/category/{category}` | GET | 否 | 查询分类文章 HTML 片段 |
| `/article/api/list/tag/{tag}` | GET | 否 | 查询标签文章 HTML 片段 |

### 1. 查询文章详情

```text
GET /article/api/data/detail/{articleId}
```

处理逻辑：

1. 根据文章 ID 查询文章基础信息和正文内容。
2. 根据当前登录用户 ID 查询用户互动状态，例如是否点赞、是否收藏。
3. 将 Markdown 正文转换为 HTML。
4. 查询作者基础信息。
5. 返回文章详情数据。

### 2. 发布文章

```text
POST /article/api/post
```

权限要求：登录用户。

处理逻辑：

1. 从用户上下文获取当前用户 ID。
2. 接收文章标题、内容、分类、标签等参数。
3. 调用 `ArticleWriteService.saveArticle` 保存文章。
4. 返回文章 ID 和文章访问标识。

简历中可以描述为：实现文章发布接口，完成参数校验、文章主表写入、文章详情写入、分类标签关联保存。

### 3. 删除文章

```text
/article/api/delete?articleId=文章ID
```

权限要求：登录用户。

处理逻辑：

1. 校验当前登录用户身份。
2. 根据文章 ID 和当前用户 ID 删除文章。
3. 通常采用逻辑删除，避免物理删除导致数据无法恢复。

### 4. 点赞收藏文章

```text
GET /article/api/favor?articleId=文章ID&type=操作类型
```

权限要求：登录用户。

处理逻辑：

1. 根据 `type` 转换为操作类型，例如点赞、取消点赞、收藏、取消收藏。
2. 校验文章是否存在。
3. 调用 `UserFootService.favorArticleComment` 更新用户行为表。
4. 可触发消息通知或排行榜热度更新。

## 六、评论接口

对应 Controller：

```text
codemate-web/src/main/java/com/github/paicoding/forum/web/front/comment/rest/CommentRestController.java
```

| 接口 | 方法 | 是否登录 | 说明 |
| --- | --- | --- | --- |
| `/comment/api/list` | GET/POST | 否 | 查询文章评论列表 |
| `/comment/api/post` | POST | 是 | 发布评论或回复 |
| `/comment/api/highlightComment` | POST | 是 | 发布划线评论 |
| `/comment/api/listTopComment` | GET | 否 | 查询顶级评论 |
| `/comment/api/delete` | GET/POST | 是 | 删除评论 |
| `/comment/api/favor` | GET | 是 | 点赞、取消点赞评论 |

### 1. 查询评论列表

```text
/comment/api/list?articleId=文章ID&pageNum=1&pageSize=10
```

处理逻辑：

1. 校验文章 ID。
2. 设置默认分页参数。
3. 查询文章下的一级评论和回复列表。
4. 返回评论树形展示所需数据。

### 2. 发布评论

```text
POST /comment/api/post
```

权限要求：登录用户。

处理逻辑：

1. 校验文章是否存在。
2. 从用户上下文获取当前用户 ID。
3. 对评论内容进行 HTML 转义，避免 XSS 风险。
4. 保存评论或回复。
5. 查询最新评论列表并返回渲染结果。

### 3. 删除评论

```text
/comment/api/delete?commentId=评论ID
```

权限要求：登录用户。

处理逻辑：

1. 校验当前登录用户是否有权删除该评论。
2. 执行评论删除逻辑。
3. 返回删除结果。

### 4. 点赞评论

```text
GET /comment/api/favor?commentId=评论ID&type=操作类型
```

权限要求：登录用户。

处理逻辑和文章点赞类似，只是 `document_type` 为评论。

## 七、用户与关注接口

对应 Controller：

```text
codemate-web/src/main/java/com/github/paicoding/forum/web/front/user/rest/UserRestController.java
```

| 接口 | 方法 | 是否登录 | 说明 |
| --- | --- | --- | --- |
| `/user/api/saveUserRelation` | POST | 是 | 关注或取消关注用户 |
| `/user/api/saveUserInfo` | POST | 是 | 修改用户资料 |
| `/user/api/articleList` | GET | 否 | 查询用户主页文章列表 |
| `/user/api/followList` | GET | 否 | 查询关注列表或粉丝列表 |

### 1. 保存关注关系

```text
POST /user/api/saveUserRelation
```

权限要求：登录用户。

处理逻辑：

1. 接收关注用户 ID 和关注状态。
2. 调用用户关系服务保存关系。
3. 更新 `user_relation` 表。
4. 后续可扩展为关注后生成站内通知。

### 2. 修改用户资料

```text
POST /user/api/saveUserInfo
```

权限要求：登录用户。

处理逻辑：

1. 校验请求中的用户 ID 是否等于当前登录用户 ID。
2. 禁止用户修改其他人的资料。
3. 保存昵称、头像、职位、公司、简介等信息。

### 3. 查询用户文章列表

```text
GET /user/api/articleList?userId=用户ID&homeSelectType=类型&page=1&pageSize=10
```

用于用户主页分页加载文章。

### 4. 查询关注或粉丝列表

```text
GET /user/api/followList?userId=用户ID&followSelectType=类型&page=1&pageSize=10
```

用于用户主页分页加载关注列表和粉丝列表。

## 八、消息通知接口

对应 Controller：

```text
codemate-web/src/main/java/com/github/paicoding/forum/web/front/notice/rest/NoticeRestController.java
```

| 接口 | 方法 | 是否登录 | 说明 |
| --- | --- | --- | --- |
| `/notice/api/list` | GET/POST | 是 | 查询通知列表 |
| `/notice/api/items` | GET/POST | 是 | 查询通知 HTML 片段 |
| `/notice/api/notifyToSelf` | GET/POST | 是 | 给自己发送测试通知 |
| `/notice/api/notifyToAll` | GET/POST | 是 | 发送广播通知 |
| `/msg/health` | WebSocket | 是 | WebSocket 健康检查 |

### 1. 查询通知列表

```text
/notice/api/list?type=通知类型&page=1&pageSize=10
```

权限要求：登录用户。

处理逻辑：

1. 校验通知类型。
2. 从用户上下文获取当前用户 ID。
3. 查询当前用户的通知列表。
4. 返回分页通知数据。

后续可以扩展：

1. 未读消息数量统计接口。
2. 单条消息已读接口。
3. 批量已读接口。
4. 按评论、点赞、收藏、关注筛选通知。

## 九、搜索接口

对应 Controller：

```text
codemate-web/src/main/java/com/github/paicoding/forum/web/front/search/rest/SearchRestController.java
```

| 接口 | 方法 | 是否登录 | 说明 |
| --- | --- | --- | --- |
| `/search/api/hint` | GET | 否 | 搜索关键词提示 |
| `/search/api/list` | GET | 否 | 搜索文章列表 |

### 1. 搜索关键词提示

```text
GET /search/api/hint?key=关键词
```

用于搜索框联想提示，根据关键词查询简要文章列表。

### 2. 搜索文章列表

```text
GET /search/api/list?key=关键词&page=1&size=10
```

用于搜索结果页分页加载文章。

后续可以改造为纯 JSON 返回，并补充标题、摘要、标签、作者等搜索维度。

## 十、排行榜接口

对应 Controller：

```text
codemate-web/src/main/java/com/github/paicoding/forum/web/front/rank/RankController.java
```

| 接口 | 方法 | 是否登录 | 说明 |
| --- | --- | --- | --- |
| `/rank/{time}` | GET/POST | 否 | 查询活跃用户排行榜页面 |

当前排行榜接口主要返回页面视图，支持按时间维度查询活跃用户排行。

后续简历版建议新增文章热度排行榜 REST 接口：

```text
GET /rank/api/articles/hot?period=day&page=1&pageSize=10
```

设计思路：

1. 通过 Redis ZSet 保存文章热度分值。
2. 定时任务从 MySQL 聚合阅读数、点赞数、收藏数、评论数。
3. 将热度分值写入 Redis。
4. 接口优先查询 Redis，返回排行榜文章列表。

## 十一、接口权限汇总

| 功能 | 是否需要登录 | 原因 |
| --- | --- | --- |
| 登录注册 | 否 | 用户进入系统入口 |
| 查询文章详情 | 否 | 内容社区基础浏览能力 |
| 查询文章列表 | 否 | 内容社区基础浏览能力 |
| 发布文章 | 是 | 需要绑定作者身份 |
| 删除文章 | 是 | 需要校验作者或管理员权限 |
| 发布评论 | 是 | 需要绑定评论用户 |
| 删除评论 | 是 | 需要校验评论作者 |
| 点赞收藏 | 是 | 需要记录用户行为状态 |
| 关注用户 | 是 | 需要记录用户关系 |
| 修改用户资料 | 是 | 只能修改自己的资料 |
| 查询通知 | 是 | 通知属于用户私有数据 |
| 搜索文章 | 否 | 公共内容检索 |
| 查看排行榜 | 否 | 公共榜单信息 |

## 十二、面试讲解版本

面试中可以这样介绍接口设计：

> CodeMate 的接口按业务域拆分，用户认证负责登录、注册和退出；文章接口负责文章详情、发布、删除、分类标签查询以及点赞收藏；评论接口负责评论列表、评论发布、评论删除和评论点赞；用户接口负责资料修改、关注关系、用户主页文章和粉丝列表；通知接口负责查询用户站内消息；搜索接口负责关键词提示和文章搜索；排行榜接口负责活跃用户排行，后续会扩展热门文章排行榜。系统通过统一响应对象 `ResVo` 封装接口结果，并通过权限注解限制发布文章、评论、点赞收藏、关注、通知查询等需要登录的操作。

也可以强调后续改造方向：

1. 将当前部分返回 HTML 片段的接口改造成纯 JSON RESTful API。
2. 补充刷新令牌与多端会话管理，完善 JWT 生命周期治理。
3. 为文章详情、文章列表、搜索、排行榜等高频接口增加 Redis 缓存。
4. 增加接口限流、操作日志和统一异常处理，提升后端系统完整度。

## 十三、后续优化计划

后续接口层可以继续做以下改造：

1. 整理 Postman 或 Apifox 接口集合。
2. 给核心接口补充请求示例和响应示例。
3. 将文章、评论、通知等接口统一改造成 JSON 返回。
4. 增加登录审计、登录限流与关键认证接口的集成测试。
5. 增加未读消息统计接口。
6. 增加热门文章排行榜接口。
7. 增加接口限流和操作日志接口切面。

## 十四、Bug 诊断与修复计划接口

Bug 诊断完成后只生成结构化预览，不会自动写任务计划。预览归属当前登录用户，确认接口使用诊断行锁、计划唯一键和审计唯一键保证重复或并发请求只创建一份计划。

```text
GET  /bug-diagnosis/api/{diagnosisId}
POST /bug-diagnosis/api/{diagnosisId}/confirm
```

确认请求为 `{"idempotencyKey":"bug-confirm-<8到64位安全字符>"}`，成功返回 `diagnosisId`、`planId` 和 `planUrl`。未登录、跨用户诊断和非法幂等键都会被拒绝；已确认诊断始终返回原计划 ID。

任务写工具为 `createTaskPlan`、`addTaskPlanStep`、`updateTaskStepStatus`、`recordTaskStepResult`。身份由服务端可信工具上下文绑定，不接受模型提供的 `userId`；每次成功写入都会记录到 `ai_task_write_audit`。

## 十五、RAG 管理与调试接口

以下接口仅管理员可访问：

```text
GET  /api/admin/ai/rag/status
POST /api/admin/ai/rag/index?articleId={articleId}
POST /api/admin/ai/rag/index-all
GET  /api/admin/ai/rag/search?question={question}&limit=10
```

调试检索返回 `vectorScore`、`keywordScore`、`freshnessScore`、最终 `score` 和 `rankingReasons`。聊天回答中的 `citations` 包含 citationIndex、articleId、chunkIndex、标题、小标题、证据摘要和相关度；这些字段只能由本次检索结果生成。
