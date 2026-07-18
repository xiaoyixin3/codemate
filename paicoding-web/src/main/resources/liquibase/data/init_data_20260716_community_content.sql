-- 首批官方社区内容：仅创建原创技术文章，不伪造用户、互动或阅读数据。
-- 每条插入均按标题去重，便于在已有数据的线上库中安全执行。

INSERT INTO `article`
(`user_id`, `article_type`, `title`, `short_title`, `url_slug`, `picture`, `summary`, `category_id`, `source`, `source_url`, `offical_stat`, `topping_stat`, `cream_stat`, `status`, `deleted`, `create_time`, `update_time`)
SELECT 1, 1, '新成员入门：用好 CodeMate 社区的第一周', '新成员入门', 'getting-started-with-codemate-community', '', '从完善主页、提出好问题到沉淀第一篇文章，给新成员的一份社区使用指南。', 4, 2, '', 1, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `article` WHERE `title` = '新成员入门：用好 CodeMate 社区的第一周' AND `deleted` = 0);

INSERT INTO `article`
(`user_id`, `article_type`, `title`, `short_title`, `url_slug`, `picture`, `summary`, `category_id`, `source`, `source_url`, `offical_stat`, `topping_stat`, `cream_stat`, `status`, `deleted`, `create_time`, `update_time`)
SELECT 1, 1, 'Spring Boot 项目上线前检查清单', '上线检查清单', 'spring-boot-production-readiness-checklist', '', '从配置、日志、健康检查到回滚预案，整理一份可直接用于发布前评审的清单。', 2, 2, '', 1, 0, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `article` WHERE `title` = 'Spring Boot 项目上线前检查清单' AND `deleted` = 0);

INSERT INTO `article`
(`user_id`, `article_type`, `title`, `short_title`, `url_slug`, `picture`, `summary`, `category_id`, `source`, `source_url`, `offical_stat`, `topping_stat`, `cream_stat`, `status`, `deleted`, `create_time`, `update_time`)
SELECT 1, 1, '给 AI 编程助手的高质量提问模板', 'AI 提问模板', 'high-quality-ai-coding-prompts', '', '用“上下文、目标、约束、验收”四段式写提示，让 AI 给出更贴近项目的答案。', 1, 2, '', 1, 0, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `article` WHERE `title` = '给 AI 编程助手的高质量提问模板' AND `deleted` = 0);

INSERT INTO `article`
(`user_id`, `article_type`, `title`, `short_title`, `url_slug`, `picture`, `summary`, `category_id`, `source`, `source_url`, `offical_stat`, `topping_stat`, `cream_stat`, `status`, `deleted`, `create_time`, `update_time`)
SELECT 1, 1, '从零设计一个可维护的 REST API', 'REST API 设计', 'maintainable-rest-api-design', '', '统一资源命名、错误结构、分页和版本策略，让接口在多人协作中仍然清晰稳定。', 2, 2, '', 1, 0, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `article` WHERE `title` = '从零设计一个可维护的 REST API' AND `deleted` = 0);

INSERT INTO `article`
(`user_id`, `article_type`, `title`, `short_title`, `url_slug`, `picture`, `summary`, `category_id`, `source`, `source_url`, `offical_stat`, `topping_stat`, `cream_stat`, `status`, `deleted`, `create_time`, `update_time`)
SELECT 1, 1, '排查线上慢接口：从现象到定位的实战路径', '慢接口排查', 'production-slow-api-troubleshooting', '', '用指标、日志、链路和 SQL 四层证据缩小范围，避免凭感觉优化。', 4, 2, '', 1, 0, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `article` WHERE `title` = '排查线上慢接口：从现象到定位的实战路径' AND `deleted` = 0);

INSERT INTO `article`
(`user_id`, `article_type`, `title`, `short_title`, `url_slug`, `picture`, `summary`, `category_id`, `source`, `source_url`, `offical_stat`, `topping_stat`, `cream_stat`, `status`, `deleted`, `create_time`, `update_time`)
SELECT 1, 1, '前端性能优化的 7 个高收益动作', '前端性能优化', 'seven-high-impact-frontend-performance-wins', '', '先测量再优化：围绕首屏、资源、渲染与缓存，优先完成投入产出比最高的改动。', 3, 2, '', 1, 0, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `article` WHERE `title` = '前端性能优化的 7 个高收益动作' AND `deleted` = 0);

INSERT INTO `article`
(`user_id`, `article_type`, `title`, `short_title`, `url_slug`, `picture`, `summary`, `category_id`, `source`, `source_url`, `offical_stat`, `topping_stat`, `cream_stat`, `status`, `deleted`, `create_time`, `update_time`)
SELECT 1, 1, '数据库索引：别只会加索引', '索引设计原则', 'database-index-design-principles', '', '理解查询路径、选择性与联合索引顺序，把索引当作一项需要验证的设计决策。', 2, 2, '', 1, 0, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `article` WHERE `title` = '数据库索引：别只会加索引' AND `deleted` = 0);

INSERT INTO `article`
(`user_id`, `article_type`, `title`, `short_title`, `url_slug`, `picture`, `summary`, `category_id`, `source`, `source_url`, `offical_stat`, `topping_stat`, `cream_stat`, `status`, `deleted`, `create_time`, `update_time`)
SELECT 1, 1, '一次代码评审应该关注什么', '代码评审指南', 'practical-code-review-guide', '', '从正确性、边界、可读性、测试和风险五个角度给出可执行的评审框架。', 4, 2, '', 1, 0, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `article` WHERE `title` = '一次代码评审应该关注什么' AND `deleted` = 0);

INSERT INTO `article`
(`user_id`, `article_type`, `title`, `short_title`, `url_slug`, `picture`, `summary`, `category_id`, `source`, `source_url`, `offical_stat`, `topping_stat`, `cream_stat`, `status`, `deleted`, `create_time`, `update_time`)
SELECT 1, 1, '开发者的周复盘：把忙碌变成可积累的成长', '开发周复盘', 'developer-weekly-retrospective', '', '用一页复盘记录目标、产出、阻塞和下周实验，让每周工作留下可复用的经验。', 4, 2, '', 1, 0, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `article` WHERE `title` = '开发者的周复盘：把忙碌变成可积累的成长' AND `deleted` = 0);

INSERT INTO `article_detail` (`article_id`, `version`, `content`, `deleted`, `create_time`, `update_time`)
SELECT a.id, 1, '# 欢迎来到 CodeMate 社区\n\n这里是开发者交流、复盘与共创的地方。社区的价值不在于信息堆积，而在于每一次提问和分享都能帮助后来的人少走一点弯路。\n\n## 第一周建议\n\n1. 完善你的个人主页，写清楚关注的技术方向。\n2. 从一个真实问题开始发帖：说明现象、已做尝试和最小复现。\n3. 阅读后留下补充或疑问，让优质内容持续生长。\n4. 完成一篇短小但完整的实践记录。\n\n请遵守基本交流原则：尊重他人、引用来源、保护密钥和用户数据。期待见到你的第一篇分享。', 0, NOW(), NOW()
FROM `article` a WHERE a.title = '新成员入门：用好 CodeMate 社区的第一周' AND NOT EXISTS (SELECT 1 FROM `article_detail` d WHERE d.article_id = a.id AND d.version = 1);

INSERT INTO `article_detail` (`article_id`, `version`, `content`, `deleted`, `create_time`, `update_time`)
SELECT a.id, 1, '# Spring Boot 项目上线前检查清单\n\n## 配置与安全\n\n- 生产环境配置与开发环境隔离，密钥只从环境变量或密钥服务读取。\n- 数据库账号使用最小权限；确认备份和恢复流程经过演练。\n- 关闭调试端点与不必要的管理入口。\n\n## 可观测性\n\n- 为核心接口补齐结构化日志、错误码和请求链路标识。\n- 配置健康检查、关键指标和告警阈值。\n- 验证日志保留周期与脱敏规则。\n\n## 发布与回滚\n\n- 用灰度发布验证核心路径：登录、下单或你的关键业务流程。\n- 为版本保留快速回滚方式，并明确负责人和触发条件。\n- 发布完成后复查错误率、延迟和资源使用情况。', 0, NOW(), NOW()
FROM `article` a WHERE a.title = 'Spring Boot 项目上线前检查清单' AND NOT EXISTS (SELECT 1 FROM `article_detail` d WHERE d.article_id = a.id AND d.version = 1);

INSERT INTO `article_detail` (`article_id`, `version`, `content`, `deleted`, `create_time`, `update_time`)
SELECT a.id, 1, '# 给 AI 编程助手的高质量提问模板\n\nAI 的输出质量高度依赖上下文。一个稳定的提示可以包含四部分：\n\n```text\n背景：项目使用 Spring Boot 2.7 和 MySQL，相关代码如下……\n目标：为接口增加分页查询。\n约束：保持现有返回结构；不要引入新依赖；兼容 Java 8。\n验收：给出修改文件、关键代码和至少两个测试场景。\n```\n\n提交前请移除 API Key、用户数据、生产地址等敏感信息。对 AI 给出的建议，仍要通过测试、代码评审和实际指标来验证。', 0, NOW(), NOW()
FROM `article` a WHERE a.title = '给 AI 编程助手的高质量提问模板' AND NOT EXISTS (SELECT 1 FROM `article_detail` d WHERE d.article_id = a.id AND d.version = 1);

INSERT INTO `article_detail` (`article_id`, `version`, `content`, `deleted`, `create_time`, `update_time`)
SELECT a.id, 1, '# 从零设计一个可维护的 REST API\n\n## 统一资源表达\n\n用名词表示资源，例如 `/api/articles` 和 `/api/articles/{id}`；操作由 HTTP 方法表达。\n\n## 统一响应与错误\n\n为响应定义稳定结构，错误中包含机器可读的错误码、给用户的提示和可追踪的 requestId。不要让异常堆栈直接暴露给调用方。\n\n## 分页与演进\n\n列表接口明确 `page`、`pageSize` 和排序规则；对数据量大的场景考虑游标分页。破坏兼容性的变更应使用版本化路径或提前提供迁移窗口。\n\n最后，把接口示例和边界条件写入文档，并为关键契约添加自动化测试。', 0, NOW(), NOW()
FROM `article` a WHERE a.title = '从零设计一个可维护的 REST API' AND NOT EXISTS (SELECT 1 FROM `article_detail` d WHERE d.article_id = a.id AND d.version = 1);

INSERT INTO `article_detail` (`article_id`, `version`, `content`, `deleted`, `create_time`, `update_time`)
SELECT a.id, 1, '# 排查线上慢接口：从现象到定位的实战路径\n\n先确认问题：是所有请求都慢、某个参数组合慢，还是某个时段慢？记录请求量、P95/P99 延迟和错误率。\n\n接着按层定位：\n\n1. **应用层**：查看线程池、GC、外部调用与慢日志。\n2. **数据库层**：对慢 SQL 使用 `EXPLAIN`，检查索引、扫描行数和锁等待。\n3. **缓存与网络**：确认命中率、连接池和下游超时。\n4. **基础设施**：检查 CPU、内存、磁盘 I/O 与网络饱和度。\n\n每次只验证一个假设，记录改动前后的指标。这样得到的不只是一次修复，而是一套可复用的排障方法。', 0, NOW(), NOW()
FROM `article` a WHERE a.title = '排查线上慢接口：从现象到定位的实战路径' AND NOT EXISTS (SELECT 1 FROM `article_detail` d WHERE d.article_id = a.id AND d.version = 1);

INSERT INTO `article_detail` (`article_id`, `version`, `content`, `deleted`, `create_time`, `update_time`)
SELECT a.id, 1, '# 前端性能优化的 7 个高收益动作\n\n1. 先用真实设备和网络测速，关注 LCP、INP、CLS。\n2. 压缩并按需加载图片，优先使用合适尺寸与现代格式。\n3. 拆分首屏以外的 JavaScript，避免一次下载所有功能。\n4. 删除未使用的依赖和样式。\n5. 为静态资源配置长期缓存和内容哈希。\n6. 减少阻塞渲染的 CSS 与第三方脚本。\n7. 在每次发布后持续比较核心页面指标。\n\n性能优化不是一次性冲刺。建立基线、设定预算、持续回归，才能防止页面在迭代中重新变慢。', 0, NOW(), NOW()
FROM `article` a WHERE a.title = '前端性能优化的 7 个高收益动作' AND NOT EXISTS (SELECT 1 FROM `article_detail` d WHERE d.article_id = a.id AND d.version = 1);

INSERT INTO `article_detail` (`article_id`, `version`, `content`, `deleted`, `create_time`, `update_time`)
SELECT a.id, 1, '# 数据库索引：别只会加索引\n\n索引的目标是减少需要扫描和排序的数据，而不是让字段“看起来更专业”。设计前先拿到真实查询：过滤条件、排序、返回列和数据分布。\n\n## 常见原则\n\n- 联合索引通常从等值过滤、选择性更高的列开始，再考虑范围条件和排序。\n- 不要为每一列都建索引；索引会增加写入成本和维护成本。\n- 用 `EXPLAIN` 验证执行计划，重点看扫描行数、使用的索引和额外排序。\n- 数据分布变化后重新验证，测试库的结论不一定能直接套到生产。\n\n把索引改动和对应查询、基准结果一起提交，团队才能理解它解决了什么问题。', 0, NOW(), NOW()
FROM `article` a WHERE a.title = '数据库索引：别只会加索引' AND NOT EXISTS (SELECT 1 FROM `article_detail` d WHERE d.article_id = a.id AND d.version = 1);

INSERT INTO `article_detail` (`article_id`, `version`, `content`, `deleted`, `create_time`, `update_time`)
SELECT a.id, 1, '# 一次代码评审应该关注什么\n\n好的评审聚焦代码的长期质量，而非个人表达。可以从这五个问题开始：\n\n- **正确性**：逻辑是否满足需求？异常、空值和并发边界如何处理？\n- **可读性**：命名、结构和注释是否让后来者看懂意图？\n- **可维护性**：是否存在重复、隐式约定或不必要的耦合？\n- **测试**：新增行为和关键失败路径是否被覆盖？\n- **风险**：是否涉及兼容性、安全、性能或数据迁移？\n\n评论尽量具体，说明原因并给出可选方向。对非阻塞建议使用明确标记，帮助作者判断优先级。', 0, NOW(), NOW()
FROM `article` a WHERE a.title = '一次代码评审应该关注什么' AND NOT EXISTS (SELECT 1 FROM `article_detail` d WHERE d.article_id = a.id AND d.version = 1);

INSERT INTO `article_detail` (`article_id`, `version`, `content`, `deleted`, `create_time`, `update_time`)
SELECT a.id, 1, '# 开发者的周复盘：把忙碌变成可积累的成长\n\n每周留出 20 分钟，用一页文档回答四个问题：\n\n1. 本周最重要的目标是什么，完成了吗？\n2. 产出了哪些可复用的代码、文档或经验？\n3. 哪个阻塞最耗时，下一次如何更早发现？\n4. 下周准备做一个什么小实验？\n\n复盘不是罗列工时。它帮助你识别重复出现的摩擦，并把临时解决方案逐渐沉淀成流程、脚本、模板或文章。欢迎在社区分享你的复盘格式和改进心得。', 0, NOW(), NOW()
FROM `article` a WHERE a.title = '开发者的周复盘：把忙碌变成可积累的成长' AND NOT EXISTS (SELECT 1 FROM `article_detail` d WHERE d.article_id = a.id AND d.version = 1);

INSERT INTO `article_tag` (`article_id`, `tag_id`, `deleted`, `create_time`, `update_time`)
SELECT a.id, t.id, 0, NOW(), NOW() FROM `article` a JOIN `tag` t ON t.tag_name = '最佳实践'
WHERE a.title IN ('新成员入门：用好 CodeMate 社区的第一周', '排查线上慢接口：从现象到定位的实战路径', '一次代码评审应该关注什么', '开发者的周复盘：把忙碌变成可积累的成长')
  AND NOT EXISTS (SELECT 1 FROM `article_tag` at WHERE at.article_id = a.id AND at.tag_id = t.id AND at.deleted = 0);

INSERT INTO `article_tag` (`article_id`, `tag_id`, `deleted`, `create_time`, `update_time`)
SELECT a.id, t.id, 0, NOW(), NOW() FROM `article` a JOIN `tag` t ON t.tag_name = 'Spring Boot'
WHERE a.title = 'Spring Boot 项目上线前检查清单'
  AND NOT EXISTS (SELECT 1 FROM `article_tag` at WHERE at.article_id = a.id AND at.tag_id = t.id AND at.deleted = 0);

INSERT INTO `article_tag` (`article_id`, `tag_id`, `deleted`, `create_time`, `update_time`)
SELECT a.id, t.id, 0, NOW(), NOW() FROM `article` a JOIN `tag` t ON t.tag_name = '大语言模型'
WHERE a.title = '给 AI 编程助手的高质量提问模板'
  AND NOT EXISTS (SELECT 1 FROM `article_tag` at WHERE at.article_id = a.id AND at.tag_id = t.id AND at.deleted = 0);
