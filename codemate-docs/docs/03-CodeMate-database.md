# CodeMate 数据库设计说明

## 一、数据库整体说明

CodeMate 技术社区后端系统使用 MySQL 作为核心业务数据库，当前本地开发库名为：

```text
pai_coding
```

项目通过 Liquibase 管理数据库初始化和后续表结构变更，初始化入口位于：

```text
paicoding-web/src/main/resources/liquibase/master.xml
paicoding-web/src/main/resources/liquibase/changelog/000_initial_schema.xml
paicoding-web/src/main/resources/liquibase/data
```

当前改造策略仍然是“项目对外叫 CodeMate，代码内部暂时保留 paicoding 和 pai_coding”，因此数据库文档先基于现有表结构进行梳理，后续再按 CodeMate 业务表达逐步优化。

## 二、核心业务表分类

核心表可以按业务域分为以下几类：

| 业务域 | 主要表 | 说明 |
| --- | --- | --- |
| 用户认证 | `user`、`user_info` | 用户登录账号、用户资料、角色信息 |
| 用户关系 | `user_relation` | 关注、取消关注、粉丝关系 |
| 用户互动 | `user_foot` | 阅读、点赞、收藏、评论等用户行为状态 |
| 文章内容 | `article`、`article_detail` | 文章基础信息、文章正文内容 |
| 分类标签 | `category`、`tag`、`article_tag` | 文章分类、标签、文章和标签的多对多关系 |
| 评论回复 | `comment` | 文章评论、二级回复 |
| 消息通知 | `notify_msg` | 评论、回复、点赞、收藏、关注、系统通知 |
| 统计计数 | `read_count`、`request_count` | 文章阅读量、系统访问计数 |
| 系统配置 | `config`、`dict_common` | 首页配置、公告、字典数据 |

## 三、用户相关表

### 1. user

`user` 是用户登录表，主要保存用户账号、密码、第三方账号标识和登录方式。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 用户主键 ID |
| `user_name` | 用户名 |
| `password` | 密码 |
| `login_type` | 登录方式 |
| `deleted` | 逻辑删除标记 |

该表更偏认证身份，适合承载登录校验、账号密码登录、第三方登录绑定等能力。

### 2. user_info

`user_info` 是用户资料表，主要保存头像、昵称、职位、公司、简介、角色和 IP 信息。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `user_id` | 关联 `user.id` |
| `user_name` | 展示昵称 |
| `photo` | 用户头像 |
| `position` | 职位 |
| `company` | 公司 |
| `profile` | 个人简介 |
| `user_role` | 用户角色，区分普通用户和管理员 |
| `ip` | 用户 IP 信息，使用 JSON 保存 |

这种拆分方式把“登录账号”和“用户资料”分开，便于后续扩展资料字段，也避免登录表越来越臃肿。

### 3. user_relation

`user_relation` 是用户关系表，用来记录用户关注关系。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `user_id` | 被关注用户 ID |
| `follow_user_id` | 发起关注的用户 ID，也就是粉丝用户 ID |
| `follow_state` | 关注状态：未关注、已关注、取消关注 |

表中通过 `uk_user_follow(user_id, follow_user_id)` 保证同一组关注关系只保留一条记录，避免重复关注产生多条脏数据。

## 四、文章相关表

### 1. article

`article` 是文章主表，保存文章标题、摘要、作者、分类、来源、状态和推荐属性。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 文章 ID |
| `user_id` | 作者用户 ID |
| `article_type` | 文章类型 |
| `title` | 文章标题 |
| `short_title` | 短标题 |
| `picture` | 文章头图 |
| `summary` | 文章摘要 |
| `category_id` | 分类 ID |
| `source` | 来源类型 |
| `offical_stat` | 官方状态 |
| `topping_stat` | 置顶状态 |
| `cream_stat` | 加精状态 |
| `status` | 发布状态 |
| `deleted` | 逻辑删除标记 |

该表适合作为文章列表页查询的数据来源，因为列表页通常只需要标题、摘要、作者、分类和状态等轻量信息。

### 2. article_detail

`article_detail` 是文章详情表，保存文章正文内容。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `article_id` | 文章 ID |
| `version` | 文章版本号 |
| `content` | 文章正文 |
| `deleted` | 逻辑删除标记 |

文章正文使用 `longtext` 存储，并与文章主表拆分。这样列表查询不需要读取大字段，能减少 IO 和网络传输开销。

表中通过 `idx_article_version(article_id, version)` 保证同一篇文章同一版本只有一条详情记录，也方便后续扩展文章版本管理。

### 3. category、tag、article_tag

`category` 保存文章分类，`tag` 保存标签，`article_tag` 保存文章和标签之间的映射关系。

关系说明：

```text
category 1 - N article
category 1 - N tag
article  1 - N article_tag
tag      1 - N article_tag
```

这种设计支持一篇文章拥有多个标签，也支持按分类和标签筛选文章。

## 五、评论相关表

`comment` 是评论表，支持文章评论和评论回复。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `article_id` | 被评论的文章 ID |
| `user_id` | 评论用户 ID |
| `content` | 评论内容 |
| `top_comment_id` | 顶级评论 ID |
| `parent_comment_id` | 父评论 ID |
| `deleted` | 逻辑删除标记 |

评论层级设计：

```text
一级评论：top_comment_id = 0，parent_comment_id = 0
二级回复：top_comment_id = 一级评论 ID，parent_comment_id = 被回复评论 ID
```

这样可以避免评论无限递归带来的复杂查询问题，列表展示时通常先查一级评论，再按 `top_comment_id` 查询该评论下的回复列表。

## 六、点赞收藏关注表

### 1. user_foot

`user_foot` 是用户行为足迹表，用一张表记录用户对文章或评论的阅读、评论、点赞、收藏状态。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `user_id` | 操作用户 ID |
| `document_id` | 文档 ID，可以是文章或评论 |
| `document_type` | 文档类型：文章或评论 |
| `document_user_id` | 内容作者 ID |
| `collection_stat` | 收藏状态 |
| `read_stat` | 阅读状态 |
| `comment_stat` | 评论状态 |
| `praise_stat` | 点赞状态 |

表中通过 `idx_user_doucument(user_id, document_id, document_type)` 保证同一用户对同一内容只有一条行为记录。

这种设计的好处是查询“当前用户是否点赞、是否收藏、是否阅读过某篇文章”比较直接，也方便后续聚合文章热度。

### 2. user_relation

关注关系不放在 `user_foot`，而是单独使用 `user_relation`，因为关注是用户和用户之间的关系，而点赞收藏是用户和内容之间的关系。

面试时可以强调这一点：不同业务关系单独建模，避免一张表承载过多语义。

## 七、消息通知表

`notify_msg` 是站内消息通知表。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `related_id` | 关联业务 ID，例如文章 ID 或评论 ID |
| `notify_user_id` | 接收通知的用户 ID |
| `operate_user_id` | 触发通知的用户 ID |
| `msg` | 通知内容 |
| `type` | 通知类型：评论、回复、点赞、收藏、关注、系统 |
| `state` | 阅读状态：未读、已读 |

表中存在 `key_notify_user_id_type_state(notify_user_id, type, state)` 索引，适合按用户、通知类型、未读状态查询消息列表或统计未读数量。

后续可以基于该表增强：

1. 查询未读消息总数。
2. 按通知类型统计未读数。
3. 批量标记消息为已读。
4. 在评论、点赞、收藏、关注等行为发生后统一生成通知。

## 八、排行榜相关数据来源

当前排行榜可以从以下数据表计算：

| 数据来源 | 可用于计算的指标 |
| --- | --- |
| `read_count` | 文章阅读量 |
| `user_foot.praise_stat` | 点赞数 |
| `user_foot.collection_stat` | 收藏数 |
| `user_foot.comment_stat` | 评论参与情况 |
| `comment` | 评论数量 |
| `article.create_time` | 新文章时间衰减权重 |

后续可以设计文章热度分值：

```text
score = 阅读数 * 1 + 点赞数 * 5 + 收藏数 * 8 + 评论数 * 10
```

然后把文章热度写入 Redis ZSet：

```text
key: codemate:rank:article:hot
member: articleId
score: 文章热度分值
```

接口查询排行榜时优先读取 Redis ZSet，定时任务定期从 MySQL 聚合数据并刷新排行榜，降低实时复杂 SQL 查询压力。

## 九、数据库初始化与版本管理

项目使用 Liquibase 管理数据库初始化流程，主要优点是：

1. 本地空库启动时可以自动执行建表和初始化数据。
2. 数据库变更脚本有版本记录，便于多人协作和环境迁移。
3. `DATABASECHANGELOG` 可以记录哪些变更已经执行，避免重复执行脚本。

之前本地空库启动时报错：

```text
Table 'pai_coding.databasechangelog' doesn't exist
```

已经通过修改数据源初始化逻辑解决：在查询 `DATABASECHANGELOG` 之前先判断表是否存在；如果不存在，则认为数据库需要初始化，让 Liquibase 继续执行建表流程。

## 十、面试讲解版本

面试中可以这样介绍数据库设计：

> CodeMate 的数据库按照社区业务域拆分，用户账号和用户资料分别放在 `user`、`user_info` 中，文章基础信息和正文内容分别放在 `article`、`article_detail` 中，避免列表查询读取大字段。文章分类和标签通过 `category`、`tag`、`article_tag` 建模，支持一篇文章多个标签。评论表通过 `top_comment_id` 和 `parent_comment_id` 支持一级评论和二级回复。点赞、收藏、阅读等用户行为统一记录在 `user_foot` 表中，并通过联合唯一索引避免重复记录。站内消息使用 `notify_msg` 表保存，支持未读状态和按用户查询。后续排行榜会基于阅读、点赞、收藏、评论等数据计算热度，并用 Redis ZSet 缓存热门文章榜单。

可以重点强调自己的改造工作：

1. 梳理社区系统核心表结构，明确用户、文章、评论、互动、通知、统计等业务边界。
2. 理解并修复 Liquibase 空库初始化流程，保证本地 MySQL 环境可以自动建表和初始化数据。
3. 设计后续 Redis 热点文章缓存和 Redis ZSet 排行榜方案，为性能优化做准备。

## 十一、后续优化计划

后续可以继续做以下数据库和缓存优化：

1. 补充 ER 图，清晰展示用户、文章、评论、互动、通知之间的关系。
2. 给核心表整理字段级说明，形成更完整的数据库字典。
3. 梳理文章列表、文章详情、评论列表、消息列表等典型 SQL 查询。
4. 为高频查询补充索引分析，说明哪些字段适合建索引。
5. 实现热点文章详情 Redis 缓存，减少文章详情对 MySQL 的访问。
6. 实现 Redis ZSet 热门文章排行榜，通过定时任务同步热度数据。
7. 优化未读消息统计，支持用户登录后快速查询未读消息数量。
