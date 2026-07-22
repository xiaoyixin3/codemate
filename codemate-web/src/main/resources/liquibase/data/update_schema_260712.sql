CREATE TABLE `ai_task_plan`
(
    `id`                   bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`              bigint unsigned NOT NULL DEFAULT '0' COMMENT '所属用户ID',
    `chat_id`              varchar(128)             DEFAULT NULL COMMENT '关联AI会话ID',
    `source_ai_type`       tinyint unsigned          DEFAULT NULL COMMENT '生成计划的AI类型',
    `title`                varchar(128) NOT NULL DEFAULT '' COMMENT '计划标题',
    `goal`                 text COMMENT '计划目标或原始需求',
    `status`               tinyint unsigned NOT NULL DEFAULT '0' COMMENT '计划状态：0草稿，1待开始，2执行中，3已完成，4已取消',
    `progress`             tinyint unsigned NOT NULL DEFAULT '0' COMMENT '计划进度，范围0-100',
    `step_total`           int unsigned NOT NULL DEFAULT '0' COMMENT '未删除步骤总数',
    `completed_step_count` int unsigned NOT NULL DEFAULT '0' COMMENT '已完成或已跳过步骤数',
    `agent_type`           varchar(64)               DEFAULT NULL COMMENT '自动执行Agent类型',
    `agent_config`         text COMMENT 'Agent配置快照，JSON文本',
    `last_execute_time`    timestamp NULL DEFAULT NULL COMMENT '最近执行时间',
    `reopen_reason`        text COMMENT '重新开启原因',
    `reopen_time`          timestamp NULL DEFAULT NULL COMMENT '最近重新开启时间',
    `extra`                text COMMENT '扩展字段，JSON文本',
    `deleted`              tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0否，1是',
    `create_time`          timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_status_update_time` (`user_id`, `status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务计划表';


CREATE TABLE `ai_task_plan_step`
(
    `id`                  bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plan_id`             bigint unsigned NOT NULL DEFAULT '0' COMMENT '所属任务计划ID',
    `user_id`             bigint unsigned NOT NULL DEFAULT '0' COMMENT '所属用户ID',
    `step_no`             int unsigned NOT NULL DEFAULT '0' COMMENT '步骤顺序，从1开始',
    `title`               varchar(256) NOT NULL DEFAULT '' COMMENT '步骤标题',
    `content`             text COMMENT '步骤说明或执行内容',
    `status`              tinyint unsigned NOT NULL DEFAULT '0' COMMENT '步骤状态：0待办，1执行中，2已完成，3阻塞，4跳过',
    `expected_output`     text COMMENT '预期输出',
    `actual_output`       text COMMENT '实际输出',
    `risk`                text COMMENT '风险说明',
    `verification_method` text COMMENT '验证方式',
    `blocked_reason`      text COMMENT '阻塞原因',
    `executor_type`       tinyint unsigned NOT NULL DEFAULT '0' COMMENT '执行者类型：0人工，1Agent',
    `agent_config`        text COMMENT '步骤Agent配置快照，JSON文本',
    `started_time`        timestamp NULL DEFAULT NULL COMMENT '开始执行时间',
    `completed_time`      timestamp NULL DEFAULT NULL COMMENT '完成时间',
    `extra`               text COMMENT '扩展字段，JSON文本',
    `deleted`             tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0否，1是',
    `create_time`         timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_id_step_no` (`plan_id`, `step_no`),
    KEY `idx_user_id_plan_id` (`user_id`, `plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务计划步骤表';
