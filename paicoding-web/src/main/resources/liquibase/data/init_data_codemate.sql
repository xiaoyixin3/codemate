-- CodeMate 的最小演示数据：分类、标签、默认管理员与三篇原创欢迎文章。

INSERT INTO `category` (`id`, `category_name`, `status`, `deleted`)
VALUES (1, 'AI 编程', 1, 0),
       (2, '后端开发', 1, 0),
       (3, '前端开发', 1, 0),
       (4, '工程实践', 1, 0);

INSERT INTO `tag` (`id`, `tag_name`, `tag_type`, `category_id`, `status`, `deleted`)
VALUES (1, 'Java', 1, 2, 1, 0),
       (2, 'Spring Boot', 1, 2, 1, 0),
       (3, 'DeepSeek', 1, 1, 1, 0),
       (4, '大语言模型', 1, 1, 1, 0),
       (5, '代码审查', 1, 4, 1, 0),
       (6, '最佳实践', 1, 4, 1, 0);

INSERT INTO `user` (`id`, `third_account_id`, `user_name`, `password`, `login_type`, `deleted`)
VALUES (1, 'codemate-admin', 'codemate-admin', 'df3a4143b663a086d1c006c8084db1b1', 0, 0);

INSERT INTO `user_info` (`id`, `user_id`, `user_name`, `photo`, `position`, `company`, `profile`, `extend`, `deleted`)
VALUES (1, 1, 'CodeMate 管理员', '/img/codemate-icon.svg', '开发者', 'CodeMate', '构建面向开发者的 AI 编程助手与技术社区。', '', 0);

INSERT INTO `article`
(`id`, `user_id`, `article_type`, `title`, `short_title`, `picture`, `summary`, `category_id`, `source`, `source_url`, `offical_stat`, `topping_stat`, `cream_stat`, `status`, `deleted`, `create_time`, `update_time`)
VALUES (100, 1, 1, '欢迎来到 CodeMate', 'CodeMate 社区', '', 'CodeMate 是一个面向开发者的 AI 编程助手与技术交流社区。', 1, 2, '', 1, 1, 1, 1, 0, NOW(), NOW()),
       (101, 1, 1, '使用 DeepSeek 开启 AI 编程对话', 'DeepSeek 配置', '', '配置 DeepSeek API Key 后，即可在 CodeMate 中体验 AI 编程对话。', 1, 2, '', 1, 0, 1, 1, 0, NOW(), NOW()),
       (102, 1, 1, 'CodeMate 项目改造路线图', '项目路线图', '', '从品牌、基础设施到 AI 会话能力，逐步把 CodeMate 建设为自己的项目。', 4, 2, '', 1, 0, 1, 1, 0, NOW(), NOW());

INSERT INTO `article_tag` (`article_id`, `tag_id`, `deleted`, `create_time`, `update_time`)
VALUES (100, 4, 0, NOW(), NOW()),
       (101, 3, 0, NOW(), NOW()),
       (101, 4, 0, NOW(), NOW()),
       (102, 6, 0, NOW(), NOW());

INSERT INTO `article_detail` (`article_id`, `version`, `content`, `deleted`, `create_time`, `update_time`)
VALUES (100, 1, '# 欢迎来到 CodeMate\n\nCodeMate 是一个面向开发者的 AI 编程助手与技术交流社区。\n\n你可以在这里发布技术文章、参与讨论，并通过 AI 助手解决编程问题。', 0, NOW(), NOW()),
       (101, 1, '# 使用 DeepSeek\n\n在开发环境中配置 DeepSeek API Key 后，启动项目并进入 AI 对话页面。\n\n请始终使用环境变量保存密钥，不要将真实密钥提交到 Git。', 0, NOW(), NOW()),
       (102, 1, '# CodeMate 路线图\n\n1. 完成个人品牌与基础配置。\n2. 清理原项目运营数据。\n3. 建设 AI 对话历史与会话管理。\n4. 部署到自己的基础设施。', 0, NOW(), NOW());
